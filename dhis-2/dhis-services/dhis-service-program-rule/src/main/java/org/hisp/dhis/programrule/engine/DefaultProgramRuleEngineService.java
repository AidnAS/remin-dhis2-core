package org.hisp.dhis.programrule.engine;

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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by zubair@dhis2.org on 23.10.17.
 */

@Slf4j
@Service( "org.hisp.dhis.programrule.engine.ProgramRuleEngineService" )
public class DefaultProgramRuleEngineService implements ProgramRuleEngineService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramRuleEngine programRuleEngine;

    private final ProgramRuleEngine programRuleEngineNew;

    private final List<RuleActionImplementer> ruleActionImplementers;

    private final ProgramInstanceService programInstanceService;

    private final ProgramStageInstanceService programStageInstanceService;

    private final ProgramService programService;

    private final ProgramRuleService programRuleService;

    public DefaultProgramRuleEngineService(
        @Qualifier( "serviceTrackerRuleEngine" ) ProgramRuleEngine programRuleEngineNew,
        @Qualifier( "notificationRuleEngine" ) ProgramRuleEngine programRuleEngine,
        List<RuleActionImplementer> ruleActionImplementers, ProgramInstanceService programInstanceService,
        ProgramStageInstanceService programStageInstanceService, ProgramRuleService programRuleService, ProgramService programService )
    {
        checkNotNull( programRuleEngine );
        checkNotNull( programRuleEngineNew );
        checkNotNull( ruleActionImplementers );
        checkNotNull( programInstanceService );
        checkNotNull( programStageInstanceService );
        checkNotNull( programRuleService );
        checkNotNull( programService );

        this.programRuleEngine = programRuleEngine;
        this.programRuleEngineNew = programRuleEngineNew;
        this.ruleActionImplementers = ruleActionImplementers;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
        this.programRuleService = programRuleService;
        this.programService = programService;
    }

    @Override
    @Transactional
    public List<RuleEffect> evaluateEnrollmentAndRunEffects( long programInstanceId )
    {
        ProgramInstance programInstance = programInstanceService.getProgramInstance( programInstanceId );

        if ( programInstance == null )
        {
            return Lists.newArrayList();
        }

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programInstance,
            programInstance.getProgramStageInstances() );

        for ( RuleEffect effect : ruleEffects )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i -> {
                log.debug( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( effect, programInstance );
            } );
        }

        return ruleEffects;
    }

    @Override
    @Transactional
    public List<RuleEffect> evaluateEventAndRunEffects( long programStageInstanceId )
    {
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( programStageInstanceId );

        return evaluateEventAndRunEffects( psi );
    }

    @Override
    @Transactional
    public List<RuleEffect> evaluateEventAndRunEffects( String event )
    {
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( event );

        return evaluateEventAndRunEffects( psi );
    }

    @Override
    public RuleValidationResult getDescription( String condition, String programId )
    {
        Program program = programService.getProgram( programId );

        return programRuleEngineNew.getDescription( condition, program );
    }

    private List<RuleEffect> evaluateEventAndRunEffects( ProgramStageInstance psi )
    {
        if ( psi == null )
        {
            return Lists.newArrayList();
        }

        ProgramInstance programInstance = programInstanceService.getProgramInstance( psi.getProgramInstance().getId() );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programInstance, psi,
                programInstance.getProgramStageInstances() );

        for ( RuleEffect effect : ruleEffects )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i -> {
                log.debug( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( effect, psi );
            } );
        }

        return ruleEffects;
    }
}
