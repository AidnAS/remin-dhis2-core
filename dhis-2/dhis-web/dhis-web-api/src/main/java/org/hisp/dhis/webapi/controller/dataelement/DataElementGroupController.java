package org.hisp.dhis.webapi.controller.dataelement;



import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.descriptors.DataElementGroupSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.controller.metadata.MetadataExportControllerUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = DataElementGroupSchemaDescriptor.API_ENDPOINT )
public class DataElementGroupController
    extends AbstractCrudController<DataElementGroup>
{
    @Autowired
    private CategoryService dataElementCategoryService;

    @Autowired
    private DataElementService dataElementService;

    @RequestMapping( value = "/{uid}/operands", method = RequestMethod.GET )
    public String getOperands( @PathVariable( "uid" ) String uid, @RequestParam Map<String, String> parameters, Model model,
        TranslateParams translateParams, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        setUserContext( translateParams );
        List<DataElementGroup> dataElementGroups = getEntity( uid, NO_WEB_OPTIONS );

        if ( dataElementGroups.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataElementGroup not found for uid: " + uid ) );
        }

        WebMetadata metadata = new WebMetadata();
        List<DataElementOperand> dataElementOperands = Lists.newArrayList( dataElementCategoryService.getOperands( dataElementGroups.get( 0 ).getMembers() ) );
        Collections.sort( dataElementOperands );

        metadata.setDataElementOperands( dataElementOperands );

        if ( options.hasPaging() )
        {
            Pager pager = new Pager( options.getPage(), dataElementOperands.size(), options.getPageSize() );
            metadata.setPager( pager );
            dataElementOperands = PagerUtils.pageCollection( dataElementOperands, pager );
        }

        metadata.setDataElementOperands( dataElementOperands );
        linkService.generateLinks( metadata, false );

        model.addAttribute( "model", metadata );
        model.addAttribute( "viewClass", options.getViewClass( "basic" ) );

        return StringUtils.uncapitalize( getEntitySimpleName() );
    }

    @RequestMapping( value = "/{uid}/operands/query/{q}", method = RequestMethod.GET )
    public String getOperandsByQuery( @PathVariable( "uid" ) String uid,
        @PathVariable( "q" ) String q, @RequestParam Map<String, String> parameters, TranslateParams translateParams, Model model,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        setUserContext( translateParams );
        List<DataElementGroup> dataElementGroups = getEntity( uid, NO_WEB_OPTIONS );

        if ( dataElementGroups.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataElementGroup not found for uid: " + uid ) );
        }

        WebMetadata metadata = new WebMetadata();
        List<DataElementOperand> dataElementOperands = Lists.newArrayList();

        for ( DataElementOperand dataElementOperand : dataElementCategoryService.getOperands( dataElementGroups.get( 0 ).getMembers() ) )
        {
            if ( dataElementOperand.getDisplayName().toLowerCase().contains( q.toLowerCase() ) )
            {
                dataElementOperands.add( dataElementOperand );
            }
        }

        metadata.setDataElementOperands( dataElementOperands );

        if ( options.hasPaging() )
        {
            Pager pager = new Pager( options.getPage(), dataElementOperands.size(), options.getPageSize() );
            metadata.setPager( pager );
            dataElementOperands = PagerUtils.pageCollection( dataElementOperands, pager );
        }

        metadata.setDataElementOperands( dataElementOperands );
        linkService.generateLinks( metadata, false );

        model.addAttribute( "model", metadata );
        model.addAttribute( "viewClass", options.getViewClass( "basic" ) );

        return StringUtils.uncapitalize( getEntitySimpleName() );
    }

    @RequestMapping( value = "/{uid}/metadata", method = RequestMethod.GET )
    public ResponseEntity<RootNode> getDataElementGroupWithDependencies( @PathVariable( "uid" ) String dataElementGroupId, @RequestParam( required = false, defaultValue = "false" ) boolean download )
        throws WebMessageException, IOException
    {
        DataElementGroup dataElementGroup = dataElementService.getDataElementGroup( dataElementGroupId );

        if ( dataElementGroup == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataElementGroup not found for uid: " + dataElementGroupId ) );
        }

        return MetadataExportControllerUtils.getWithDependencies( contextService, exportService, dataElementGroup, download );
    }
}
