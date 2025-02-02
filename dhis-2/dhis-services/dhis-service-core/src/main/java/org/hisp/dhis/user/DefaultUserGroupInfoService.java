package org.hisp.dhis.user;
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

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.commons.util.SystemUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class DefaultUserGroupInfoService implements UserGroupInfoService
{
    private final Environment env;

    private final CacheProvider cacheProvider;

    private final UserGroupInfoStore userGroupInfoStore;

    /**
     * Cache for checking if user is member of a UserGroup
     * Cache key userGroupUid-userUid
     */
    private Cache<Boolean> IS_USER_GROUP_MEMBER;

    @PostConstruct
    public void init()
    {
        IS_USER_GROUP_MEMBER = cacheProvider.newCacheBuilder( Boolean.class )
            .forRegion( "isUserGroupMember" )
            .expireAfterWrite( 3, TimeUnit.HOURS )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( SystemUtils.isTestRun( env.getActiveProfiles() ) ? 0 : 1000000 ).build();
    }

    public DefaultUserGroupInfoService( Environment env, CacheProvider cacheProvider,
        UserGroupInfoStore userGroupInfoStore )
    {
        checkNotNull( env );
        checkNotNull( cacheProvider );
        checkNotNull( userGroupInfoStore );

        this.env = env;
        this.cacheProvider = cacheProvider;
        this.userGroupInfoStore = userGroupInfoStore;
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isMember( UserGroup userGroup, String userUid )
    {
        return IS_USER_GROUP_MEMBER.get( userGroup.getUid() + "-" + userUid, bol -> userGroupInfoStore.isMember( userGroup, userUid ) ).get();
    }
}
