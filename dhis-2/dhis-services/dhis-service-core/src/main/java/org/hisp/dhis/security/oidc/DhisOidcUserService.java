package org.hisp.dhis.security.oidc;

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
 *
 */

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service( "dhisOidcUserService" )
public class DhisOidcUserService
    extends OidcUserService
{
    @Autowired
    public UserService userService;

    @Autowired
    private DhisClientRegistrationRepository clientRegistrationRepository;

    @Override
    public OidcUser loadUser( OidcUserRequest userRequest )
        throws OAuth2AuthenticationException
    {
        ClientRegistration clientRegistration = userRequest.getClientRegistration();
        DhisOidcClientRegistration oidcClientRegistration = clientRegistrationRepository
            .getDhisOidcClientRegistration( clientRegistration.getRegistrationId() );

        OidcUser oidcUser = super.loadUser( userRequest );
        Map<String, Object> attributes = oidcUser.getAttributes();

        String mappingClaimKey = oidcClientRegistration.getMappingClaimKey();
        OidcUserInfo userInfo = oidcUser.getUserInfo();
        Object claimValue = attributes.get( mappingClaimKey );

        if ( claimValue == null && userInfo != null )
        {
            claimValue = userInfo.getClaim( mappingClaimKey );
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "Trying to look up DHIS2 user with OidcUser mapping, claim value:" + claimValue );
        }

        if ( claimValue != null )
        {
            UserCredentials userCredentials = userService.getUserCredentialsByOpenId( (String) claimValue );

            if ( userCredentials != null )
            {
                return new DhisOidcUser( userCredentials, attributes, IdTokenClaimNames.SUB, oidcUser.getIdToken() );
            }
        }

        if ( log.isInfoEnabled() )
        {
            log.info( "Failed to look up DHIS2 user with OidcUser mapping, claim value:" + claimValue );
        }

        OAuth2Error oauth2Error = new OAuth2Error(
            "could_not_map_oidc_user_to_dhis2_user",
            "Failed to map OidcUser to a DHIS2 user.",
            null );

        throw new OAuth2AuthenticationException( oauth2Error, oauth2Error.toString() );
    }
}
