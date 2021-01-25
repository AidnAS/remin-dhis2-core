package org.hisp.dhis.commons.action;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.security.authority.SystemAuthoritiesProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hisp.dhis.schema.descriptors.ChartSchemaDescriptor.F_CHART_EXTERNAL;
import static org.hisp.dhis.schema.descriptors.ChartSchemaDescriptor.F_CHART_PUBLIC_ADD;
import static org.hisp.dhis.schema.descriptors.ReportTableSchemaDescriptor.F_REPORTTABLE_EXTERNAL;
import static org.hisp.dhis.schema.descriptors.ReportTableSchemaDescriptor.F_REPORTTABLE_PUBLIC_ADD;

/**
 * @author mortenoh
 */
public class GetSystemAuthoritiesAction
    extends ActionPagingSupport<String>
{

    @Deprecated
    private static final List<String> DEPRECATED_SCHEMAS = asList( F_REPORTTABLE_EXTERNAL, F_REPORTTABLE_PUBLIC_ADD,
        F_CHART_EXTERNAL, F_CHART_PUBLIC_ADD );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SystemAuthoritiesProvider authoritiesProvider;

    public void setAuthoritiesProvider( SystemAuthoritiesProvider authoritiesProvider )
    {
        this.authoritiesProvider = authoritiesProvider;
    }

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String systemAuthorities;

    public String getSystemAuthorities()
    {
        return systemAuthorities;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode authNodes = mapper.createArrayNode();

        List<String> listAuthorities =  new ArrayList<>( authoritiesProvider.getSystemAuthorities() );
        Collections.sort( listAuthorities );

        if ( usePaging )
        {
            this.paging = createPaging( listAuthorities.size() );

            listAuthorities = listAuthorities.subList( paging.getStartPos(), paging.getEndPos() );
        }

        listAuthorities.forEach( auth -> {
            String name = getAuthName( auth );

            if ( isNotDeprecated( auth ) )
            {
                authNodes.add( mapper.createObjectNode().put( "id", auth ).put( "name", name ) );
            }
        } );

        root.set( "systemAuthorities", authNodes );

        systemAuthorities = mapper.writeValueAsString( root );

        return SUCCESS;
    }

    private String getAuthName( String auth )
    {
        auth = i18n.getString( auth );

        // Custom App doesn't have translation for See App authority
        if ( auth.startsWith( App.SEE_APP_AUTHORITY_PREFIX ) )
        {
            auth = auth.replaceFirst( App.SEE_APP_AUTHORITY_PREFIX, "" ).replaceAll( "_", " " ) + " app";
        }

        return auth;
    }

    /**
     * This checking is required in order to "temporally" remove the deprecated schemas.
     * Created and used during the transition from Chart/ReportTable to Visualization.
     *
     * @param authId to be filtered out if the same is deprecated.
     * @return true if the authId is NOT deprecated, false otherwise.
     */
    @Deprecated
    private boolean isNotDeprecated( final String authId )
    {
        return !DEPRECATED_SCHEMAS.contains( authId );
    }
}
