/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.table;

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.analytics.ColumnDataType.*;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ConcurrentUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.CompletenessTableManager" )
public class JdbcCompletenessTableManager
    extends AbstractJdbcTableManager
{
    public JdbcCompletenessTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate );
    }

    private static final List<AnalyticsTableColumn> FIXED_COLS = Lists.newArrayList(
        new AnalyticsTableColumn( quote( "dx" ), CHARACTER_11, NOT_NULL, "ds.uid" ),
        new AnalyticsTableColumn( quote( "year" ), INTEGER, NOT_NULL, "ps.year" ) );

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.COMPLETENESS;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        AnalyticsTable table = params.isLatestUpdate()
            ? getLatestAnalyticsTable( params, getDimensionColumns(), getValueColumns() )
            : getRegularAnalyticsTable( params, getDataYears( params ), getDimensionColumns(), getValueColumns() );

        return table.hasPartitionTables() ? Lists.newArrayList( table ) : Lists.newArrayList();
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return Sets.newHashSet( getTableName() );
    }

    @Override
    public String validState()
    {
        boolean hasData = jdbcTemplate.queryForRowSet( "select datasetid from completedatasetregistration limit 1" )
            .next();

        if ( !hasData )
        {
            return "No complete registrations exist, not updating completeness analytics tables";
        }

        return null;
    }

    @Override
    protected boolean hasUpdatedLatestData( Date startDate, Date endDate )
    {
        String sql = "select cdr.datasetid " +
            "from completedatasetregistration cdr " +
            "where cdr.lastupdated >= '" + getLongDateString( startDate ) + "' " +
            "and cdr.lastupdated < '" + getLongDateString( endDate ) + "' " +
            "limit 1";

        return !jdbcTemplate.queryForList( sql ).isEmpty();
    }

    @Override
    public void removeUpdatedData( AnalyticsTableUpdateParams params, List<AnalyticsTable> tables )
    {
        if ( !params.isLatestUpdate() )
        {
            return;
        }

        AnalyticsTablePartition partition = PartitionUtils.getLatestTablePartition( tables );

        String sql = "delete from " + quote( getAnalyticsTableType().getTableName() ) + " ax " +
            "where ax.id in (" +
            "select (ds.uid || '-' || ps.iso || '-' || ou.uid || '-' || ao.uid) as id " +
            "from completedatasetregistration cdr " +
            "inner join dataset ds on cdr.datasetid=ds.datasetid " +
            "inner join _periodstructure ps on cdr.periodid=ps.periodid " +
            "inner join organisationunit ou on cdr.sourceid=ou.organisationunitid " +
            "inner join categoryoptioncombo ao on cdr.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "where cdr.lastupdated >= '" + getLongDateString( partition.getStartDate() ) + "' " +
            "and cdr.lastupdated < '" + getLongDateString( partition.getEndDate() ) + "')";

        invokeTimeAndLog( sql, "Remove updated data values" );
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return partition.isLatestPartition() ? newArrayList()
            : Lists.newArrayList( "year = " + partition.getYear() + "" );
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        final String tableName = partition.getTempTableName();
        final String partitionClause = partition.isLatestPartition()
            ? "and cdr.lastupdated >= '" + getLongDateString( partition.getStartDate() ) + "' "
            : "and ps.year = " + partition.getYear() + " ";

        String insert = "insert into " + partition.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();
        List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : ListUtils.union( columns, values ) )
        {
            insert += col.getName() + ",";
        }

        insert = TextUtils.removeLastComma( insert ) + ") ";

        String select = "select ";

        for ( AnalyticsTableColumn col : columns )
        {
            select += col.getAlias() + ",";
        }

        select = select.replace( "organisationunitid", "sourceid" ); // Database
                                                                     // legacy
                                                                     // fix

        select += "cdr.date as value " +
            "from completedatasetregistration cdr " +
            "inner join dataset ds on cdr.datasetid=ds.datasetid " +
            "inner join period pe on cdr.periodid=pe.periodid " +
            "inner join _periodstructure ps on cdr.periodid=ps.periodid " +
            "inner join organisationunit ou on cdr.sourceid=ou.organisationunitid " +
            "inner join _organisationunitgroupsetstructure ougs on cdr.sourceid=ougs.organisationunitid " +
            "and (cast(date_trunc('month', pe.startdate) as date)=ougs.startdate or ougs.startdate is null) " +
            "left join _orgunitstructure ous on cdr.sourceid=ous.organisationunitid " +
            "inner join _categorystructure acs on cdr.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "inner join categoryoptioncombo ao on cdr.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "where cdr.date is not null " +
            partitionClause +
            "and cdr.lastupdated < '" + getLongDateString( params.getStartTime() ) + "' " +
            "and cdr.completed = true";

        final String sql = insert + select;

        invokeTimeAndLog( sql, String.format( "Populate %s", tableName ) );
    }

    private List<AnalyticsTableColumn> getDimensionColumns()
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        String idColAlias = "(ds.uid || '-' || ps.iso || '-' || ou.uid || '-' || ao.uid) as id ";
        columns.add( new AnalyticsTableColumn( quote( "id" ), ColumnDataType.TEXT, idColAlias ) );

        List<OrganisationUnitGroupSet> orgUnitGroupSets = idObjectManager
            .getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<OrganisationUnitLevel> levels = organisationUnitService.getFilledOrganisationUnitLevels();

        List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets = categoryService
            .getAttributeCategoryOptionGroupSetsNoAcl();

        List<Category> attributeCategories = categoryService.getAttributeDataDimensionCategoriesNoAcl();

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), CHARACTER_11,
                "ougs." + quote( groupSet.getUid() ) ).withCreated( groupSet.getCreated() ) );
        }

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add(
                new AnalyticsTableColumn( column, CHARACTER_11, "ous." + column ).withCreated( level.getCreated() ) );
        }

        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), CHARACTER_11,
                "acs." + quote( groupSet.getUid() ) ).withCreated( groupSet.getCreated() ) );
        }

        for ( Category category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), CHARACTER_11,
                "acs." + quote( category.getUid() ) ).withCreated( category.getCreated() ) );
        }

        columns.addAll( addPeriodColumns( "ps" ) );

        String timelyDateDiff = statementBuilder.getDaysBetweenDates( "pe.enddate",
            statementBuilder.getCastToDate( "cdr.date" ) );
        String timelyAlias = "(select (" + timelyDateDiff + ") <= ds.timelydays) as timely";

        columns.add( new AnalyticsTableColumn( quote( "timely" ), BOOLEAN, timelyAlias ) );
        columns.addAll( getFixedColumns() );
        return filterDimensionColumns( columns );
    }

    private List<AnalyticsTableColumn> getValueColumns()
    {
        return Lists.newArrayList( new AnalyticsTableColumn( quote( "value" ), DATE, "value" ) );
    }

    private List<Integer> getDataYears( AnalyticsTableUpdateParams params )
    {
        String sql = "select distinct(extract(year from pe.startdate)) " +
            "from completedatasetregistration cdr " +
            "inner join period pe on cdr.periodid=pe.periodid " +
            "where pe.startdate is not null " +
            "and cdr.date < '" + getLongDateString( params.getStartTime() ) + "' ";

        if ( params.getFromDate() != null )
        {
            sql += "and pe.startdate >= '" + DateUtils.getMediumDateString( params.getFromDate() ) + "'";
        }

        return jdbcTemplate.queryForList( sql, Integer.class );
    }

    @Override
    @Async
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions,
        Collection<String> dataElements, int aggregationLevel )
    {
        return ConcurrentUtils.getImmediateFuture();
    }

    @Override
    @Async
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions )
    {
        return ConcurrentUtils.getImmediateFuture();
    }

    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return FIXED_COLS;
    }
}
