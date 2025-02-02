package org.hisp.dhis.programrule;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author markusbekken
 */
@Component( "org.hisp.dhis.programrule.ProgramRuleVariableDeletionHandler" )
public class ProgramRuleVariableDeletionHandler
    extends DeletionHandler 
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramRuleVariableService programRuleVariableService;

    public ProgramRuleVariableDeletionHandler( ProgramRuleVariableService programRuleVariableService )
    {
        checkNotNull( programRuleVariableService );
        this.programRuleVariableService = programRuleVariableService;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------
    @Override
    protected String getClassName()
    {
        return ProgramRuleVariable.class.getSimpleName();
    }

    @Override
    public String allowDeleteProgramStage( ProgramStage programStage )
    {
        String programRuleVariables = programRuleVariableService
            .getProgramRuleVariable( programStage.getProgram() )
            .stream()
            .filter( prv -> Objects.equals( prv.getProgramStage(), programStage ) )
            .map( BaseIdentifiableObject::getName )
            .collect( Collectors.joining( ", " ) );

        return StringUtils.isBlank( programRuleVariables ) ? null : programRuleVariables;
    }
    
    @Override
    public void deleteProgram( Program program )
    {
        for ( ProgramRuleVariable programRuleVariable : programRuleVariableService.getProgramRuleVariable( program ) )
        {
            programRuleVariableService.deleteProgramRuleVariable( programRuleVariable );
        }
    }
}
