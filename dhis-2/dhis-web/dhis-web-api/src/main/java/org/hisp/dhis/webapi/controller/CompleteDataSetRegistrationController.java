package org.hisp.dhis.webapi.controller;



import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.DefaultCompleteDataSetRegistrationExchangeService;
import org.hisp.dhis.dxf2.dataset.ExportParams;
import org.hisp.dhis.dxf2.dataset.tasks.ImportCompleteDataSetRegistrationsTask;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.COMPLETE_DATA_SET_REGISTRATION_IMPORT;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Halvdan Hoem Grelland <halvdan@dhis2.org>
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequestMapping( value = CompleteDataSetRegistrationController.RESOURCE_PATH )
public class CompleteDataSetRegistrationController
{
    public static final String RESOURCE_PATH = "/completeDataSetRegistrations";

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private InputUtils inputUtils;

    @Autowired
    private DefaultCompleteDataSetRegistrationExchangeService registrationExchangeService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private WebMessageService webMessageService;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_XML )
    public void getCompleteRegistrationsXml(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false, name = "children" ) boolean includeChildren,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Date created,
        @RequestParam( required = false ) String createdDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletRequest request,
        HttpServletResponse response
    )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_XML );

        ExportParams params = registrationExchangeService.paramsFromUrl(
            dataSet, orgUnit, orgUnitGroup, period, startDate, endDate, includeChildren, created, createdDuration, limit, idSchemes );

        registrationExchangeService.writeCompleteDataSetRegistrationsXml( params, response.getOutputStream() );
    }

    @RequestMapping( method = RequestMethod.GET, produces = CONTENT_TYPE_JSON )
    public void getCompleteRegistrationsJson(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false, name = "children" ) boolean includeChildren,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Date created,
        @RequestParam( required = false ) String createdDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletRequest request,
        HttpServletResponse response
    )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );

        ExportParams params = registrationExchangeService.paramsFromUrl(
            dataSet, orgUnit, orgUnitGroup, period, startDate, endDate, includeChildren, created, createdDuration, limit, idSchemes );

        registrationExchangeService.writeCompleteDataSetRegistrationsJson( params, response.getOutputStream() );
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, consumes = CONTENT_TYPE_XML )
    public void postCompleteRegistrationsXml(
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response
    )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            asyncImport( importOptions, ImportCompleteDataSetRegistrationsTask.FORMAT_XML, request, response );
        }
        else
        {
            response.setContentType( CONTENT_TYPE_XML );
            ImportSummary summary = registrationExchangeService.saveCompleteDataSetRegistrationsXml( request.getInputStream(), importOptions );
            summary.setImportOptions( importOptions );
            renderService.toXml( response.getOutputStream(), summary );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = CONTENT_TYPE_JSON )
    public void postCompleteRegistrationsJson(
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response
    )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            asyncImport( importOptions, ImportCompleteDataSetRegistrationsTask.FORMAT_JSON, request, response );
        }
        else
        {
            response.setContentType( CONTENT_TYPE_JSON );
            ImportSummary summary = registrationExchangeService.saveCompleteDataSetRegistrationsJson( request.getInputStream(), importOptions );
            summary.setImportOptions( importOptions );
            renderService.toJson( response.getOutputStream(), summary );
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteCompleteDataSetRegistration(
        @RequestParam Set<String> ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam( required = false ) boolean multiOu, HttpServletResponse response ) throws WebMessageException
    {
        Set<DataSet> dataSets = new HashSet<>( manager.getByUid( DataSet.class, ds ) );

        if ( dataSets.size() != ds.size() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal data set identifier in this list: " + ds ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal period identifier: " + pe ) );
        }

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal organisation unit identifier: " + ou ) );
        }

        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( cc, cp, false );

        if ( attributeOptionCombo == null )
        {
            return;
        }

        // ---------------------------------------------------------------------
        // Check locked status
        // ---------------------------------------------------------------------

        User user = currentUserService.getCurrentUser();

        List<String> lockedDataSets = new ArrayList<>();

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSetService.isLocked( user, dataSet, period, organisationUnit, attributeOptionCombo, null, multiOu ) )
            {
                lockedDataSets.add( dataSet.getUid() );
            }
        }

        if ( lockedDataSets.size() != 0 )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Locked Data set(s) : " + StringUtils.join( lockedDataSets, ", " ) ) );
        }

        // ---------------------------------------------------------------------
        // Un-register as completed data set
        // ---------------------------------------------------------------------

        Set<OrganisationUnit> orgUnits = new HashSet<>();
        orgUnits.add( organisationUnit );

        if ( multiOu )
        {
            orgUnits.addAll( organisationUnit.getChildren() );
        }

        unRegisterCompleteDataSet( dataSets, period, orgUnits, attributeOptionCombo );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void asyncImport( ImportOptions importOptions, String format, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        Pair<InputStream, Path> tmpFile = saveTmpFile( request.getInputStream() );

        JobConfiguration jobId = new JobConfiguration( "inMemoryCompleteDataSetRegistrationImport", COMPLETE_DATA_SET_REGISTRATION_IMPORT, currentUserService.getCurrentUser().getUid(), true );

        schedulingManager.executeJob(
            new ImportCompleteDataSetRegistrationsTask(
                registrationExchangeService, sessionFactory, tmpFile.getLeft(), tmpFile.getRight(), importOptions, format,
                jobId )
        );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + COMPLETE_DATA_SET_REGISTRATION_IMPORT );
        webMessageService.send( jobConfigurationReport( jobId ), response, request );
    }

    private Pair<InputStream, Path> saveTmpFile( InputStream in )
        throws IOException
    {
        String filename = CodeGenerator.generateCode( 6 );

        File tmpFile = File.createTempFile( filename, null );
        tmpFile.deleteOnExit();

        try ( FileOutputStream out = new FileOutputStream( tmpFile ) )
        {
            IOUtils.copy( in, out );
        }

        return Pair.of( new BufferedInputStream( new FileInputStream( tmpFile ) ), tmpFile.toPath() );
    }

    private void unRegisterCompleteDataSet( Set<DataSet> dataSets, Period period,
        Set<OrganisationUnit> orgUnits, CategoryOptionCombo attributeOptionCombo )
    {
        List<CompleteDataSetRegistration> registrations = new ArrayList<>();

        for ( OrganisationUnit unit : orgUnits )
        {
            for ( DataSet dataSet : dataSets )
            {
                if ( unit.getDataSets().contains( dataSet ) )
                {
                    CompleteDataSetRegistration registration = registrationService
                        .getCompleteDataSetRegistration( dataSet, period, unit, attributeOptionCombo );

                    if ( registration != null )
                    {
                        registrations.add( registration );
                    }
                }
            }
        }
        if ( !registrations.isEmpty() )
        {
            registrationService.deleteCompleteDataSetRegistrations( registrations );
        }
    }
}
