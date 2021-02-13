package org.hisp.dhis.webapi.controller.dataitem;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE_SIZE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.cache.CacheBuilder;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dataitem.query.QueryExecutor;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class ResponseHandlerTest
{
    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private LinkService linkService;

    @Mock
    private FieldFilterService fieldFilterService;

    @Mock
    private Environment environment;

    @Mock
    private CacheProvider cacheProvider;

    @Rule
    public MockitoRule mockitoRule = rule();

    private ResponseHandler responseHandler;

    @Before
    public void setUp()
    {
        responseHandler = new ResponseHandler( queryExecutor, linkService, fieldFilterService, environment,
            cacheProvider );
    }

    @Test
    public void testAddResultsToNodeWithSuccess()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final DataItem anyDataItem = new DataItem();
        anyDataItem.setName( "any" );
        final List<DataItem> anyDimensionalItems = singletonList( anyDataItem );
        final Set<String> anyFields = newHashSet( "name" );
        final CollectionNode anyCollectionNode = new CollectionNode( "any" );

        // When
        when( fieldFilterService.toConcreteClassCollectionNode( any(), any(), any(), any() ) )
            .thenReturn( anyCollectionNode );
        responseHandler.addResultsToNode( anyRootNode, anyDimensionalItems, anyFields );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), hasSize( 1 ) );
        assertThat( anyRootNode.getChildren().get( 0 ).isCollection(), is( true ) );
    }

    @Test
    public void testAddPaginationToNodeWithSuccess()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final List<Class<? extends BaseIdentifiableObject>> anyTargetEntities = asList( Indicator.class,
            DataSet.class );
        final Set<String> anyFilters = newHashSet( "any" );
        final User anyUser = new User();
        final WebOptions anyWebOptions = mockWebOptions( 10, 1 );
        final String[] testEnvironmentVars = { "test" };
        final CacheBuilder<Integer> testingCacheBuilder = new SimpleCacheBuilder<>();

        // When
        when( environment.getActiveProfiles() ).thenReturn( testEnvironmentVars );
        when( cacheProvider.newCacheBuilder( Integer.class ) ).thenReturn( testingCacheBuilder );
        responseHandler.init();
        responseHandler.addPaginationToNode( anyRootNode, anyTargetEntities, anyUser, anyWebOptions,
            anyFilters );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), hasSize( 1 ) );
        assertThat( anyRootNode.getChildren().get( 0 ).isMetadata(), is( true ) );
        assertThat( anyRootNode.getChildren().get( 0 ).isComplex(), is( true ) );
        verify( linkService, times( 1 ) ).generatePagerLinks( any( Pager.class ), anyString() );
    }

    @Test
    public void testAddPaginationToNodeWhenPagingIsFalse()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final List<Class<? extends BaseIdentifiableObject>> anyTargetEntities = asList( Indicator.class,
            DataSet.class );
        final Set<String> anyFilters = newHashSet( "any" );
        final User anyUser = new User();
        final WebOptions webOptionsNoPaging = mockWebOptionsNoPaging();
        final String[] testEnvironmentVars = { "test" };
        final CacheBuilder<Integer> testingCacheBuilder = new SimpleCacheBuilder<>();

        // When
        when( environment.getActiveProfiles() ).thenReturn( testEnvironmentVars );
        when( cacheProvider.newCacheBuilder( Integer.class ) ).thenReturn( testingCacheBuilder );
        responseHandler.init();
        responseHandler.addPaginationToNode( anyRootNode, anyTargetEntities, anyUser, webOptionsNoPaging,
            anyFilters );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), is( empty() ) );
        verify( linkService, never() ).generatePagerLinks( any( Pager.class ), anyString() );
    }

    @Test
    public void testAddPaginationToNodeWhenTargetEntitiesIsEmpty()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final List<Class<? extends BaseIdentifiableObject>> emptyTargetEntities = emptyList();
        final Set<String> anyFilters = newHashSet( "any" );
        final User anyUser = new User();
        final WebOptions anyWebOptions = mockWebOptions( 10, 1 );
        final String[] testEnvironmentVars = { "test" };
        final CacheBuilder<Integer> testingCacheBuilder = new SimpleCacheBuilder<>();

        // When
        when( environment.getActiveProfiles() ).thenReturn( testEnvironmentVars );
        when( cacheProvider.newCacheBuilder( Integer.class ) ).thenReturn( testingCacheBuilder );
        responseHandler.init();
        responseHandler.addPaginationToNode( anyRootNode, emptyTargetEntities, anyUser, anyWebOptions,
            anyFilters );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), is( empty() ) );
        verify( linkService, never() ).generatePagerLinks( any( Pager.class ), anyString() );
        verify( queryExecutor, never() ).count( any( Class.class ), any( MapSqlParameterSource.class ) );
    }

    private WebOptions mockWebOptions( final int pageSize, final int pageNumber )
    {
        final Map<String, String> options = new HashMap<>( 0 );
        options.put( PAGE_SIZE, valueOf( pageSize ) );
        options.put( PAGE, valueOf( pageNumber ) );
        options.put( PAGING, "true" );

        return new WebOptions( options );
    }

    private WebOptions mockWebOptionsNoPaging()
    {
        final Map<String, String> options = new HashMap<>( 0 );
        options.put( PAGING, "false" );

        return new WebOptions( options );
    }
}
