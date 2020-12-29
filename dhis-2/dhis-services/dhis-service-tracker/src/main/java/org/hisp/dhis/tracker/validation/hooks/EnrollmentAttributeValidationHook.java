package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1017;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1018;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1019;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1075;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1076;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentAttributeValidationHook extends AttributeValidationHook
{

    public EnrollmentAttributeValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( teAttrService );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        TrackedEntityInstance tei = context.getTrackedEntityInstance( enrollment.getTrackedEntity() );

        OrganisationUnit orgUnit = context
            .getOrganisationUnit( getOrgUnitUidFromTei( context, enrollment.getTrackedEntity() ) );

        Map<String, String> attributeValueMap = Maps.newHashMap();

        for ( Attribute attribute : enrollment.getAttributes() )
        {
            validateRequiredProperties( reporter, attribute );

            if ( attribute.getAttribute() == null || attribute.getValue() == null ||
                context.getTrackedEntityAttribute( attribute.getAttribute() ) == null )
            {
                continue;
            }

            attributeValueMap.put( attribute.getAttribute(), attribute.getValue() );

            TrackedEntityAttribute teAttribute = context.getTrackedEntityAttribute( attribute.getAttribute() );

            validateAttrValueType( reporter, attribute, teAttribute );

            validateAttributeUniqueness( reporter,
                attribute.getValue(),
                teAttribute,
                tei,
                orgUnit );
        }

        Program program = context.getProgram( enrollment.getProgram() );
        validateMandatoryAttributes( reporter, program, enrollment );
    }

    protected void validateRequiredProperties( ValidationErrorReporter reporter, Attribute attribute )
    {
        addErrorIfNull( attribute.getAttribute(), reporter, E1075, attribute );
        addErrorIfNull( attribute.getValue(), reporter, E1076, attribute );

        if ( attribute.getAttribute() != null )
        {
            TrackedEntityAttribute teAttribute = reporter.getValidationContext()
                .getTrackedEntityAttribute( attribute.getAttribute() );

            addErrorIfNull( teAttribute, reporter, E1017, attribute );
        }
    }

    private void validateMandatoryAttributes( ValidationErrorReporter reporter,
        Program program, Enrollment enrollment )
    {
        checkNotNull( program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL );

        // Build a data structures of attributes eligible for mandatory validations:
        // 1 - attributes from enrollments whose value is not empty or null
        // 2 - attributes already existing in TEI (from preheat)

        // 1 - attributes from enrollment whose value is non-empty
        Map<String, String> enrollmentNonEmptyAttributeUids = Optional.of( enrollment )
            .map( Enrollment::getAttributes )
            .orElse( Collections.emptyList() )
            .stream()
            .filter( this::isNonEmpty)
            .collect( Collectors.toMap(
                Attribute::getAttribute,
                Attribute::getValue ) );

        // 2 - attributes uids from existing TEI (if any) from preheat
        Set<String> teiAttributeUids = buildTeiAttributeUids( reporter, enrollment.getTrackedEntity() );

        // merged uids of eligible attribute to validate
        Set<String> mergedAttributes = Streams
            .concat( enrollmentNonEmptyAttributeUids.keySet().stream(), teiAttributeUids.stream() )
            .collect( Collectors.toSet() );

        // Map having as key program attribute uid and mandatory flag as value
        Map<String, Boolean> programAttributesMap = program.getProgramAttributes().stream()
            .collect( Collectors.toMap(
                programTrackedEntityAttribute -> programTrackedEntityAttribute.getAttribute().getUid(),
                ProgramTrackedEntityAttribute::isMandatory ) );

        // Merged attributes must contain each mandatory program attribute.
        programAttributesMap.entrySet()
            .stream()
            .filter( Map.Entry::getValue ) // <--- filter on mandatory flag
            .map( Map.Entry::getKey )
            .forEach( mandatoryProgramAttributeUid -> addErrorIf(
                () -> !mergedAttributes.contains( mandatoryProgramAttributeUid ), reporter, E1018,
                mandatoryProgramAttributeUid, program.getUid(), enrollment.getEnrollment() ) );

        // enrollment must not contain any attribute which is not defined in program
        enrollmentNonEmptyAttributeUids
            .forEach(
                ( attrUid, attrVal ) -> addErrorIf( () -> !programAttributesMap.containsKey( attrUid ), reporter, E1019,
                    attrUid + "=" + attrVal ) );
    }

    private Set<String> buildTeiAttributeUids( ValidationErrorReporter reporter, String trackedEntityInstanceUid )
    {
        return Optional.of( reporter )
            .map( ValidationErrorReporter::getValidationContext )
            .map( TrackerImportValidationContext::getBundle )
            .map( TrackerBundle::getPreheat )
            .map( trackerPreheat -> trackerPreheat.getTrackedEntity( TrackerIdScheme.UID, trackedEntityInstanceUid ) )
            .map( TrackedEntityInstance::getTrackedEntityAttributeValues )
            .orElse( Collections.emptySet() )
            .stream()
            .map( TrackedEntityAttributeValue::getAttribute )
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toSet() );
    }

    private boolean isNonEmpty(Attribute attribute )
    {
        return StringUtils.isNotBlank( attribute.getValue() ) && StringUtils.isNotBlank(attribute.getAttribute());
    }

    private String getOrgUnitUidFromTei( TrackerImportValidationContext context, String teiUid )
    {

        final Optional<ReferenceTrackerEntity> reference = context.getReference( teiUid );
        if ( reference.isPresent() )
        {
            final Optional<TrackedEntity> tei = context.getBundle()
                .getTrackedEntity( teiUid );
            if ( tei.isPresent() )
            {
                return tei.get().getOrgUnit();
            }
        }
        return null;
    }
}