-- These indexes are added in future commits to 2.35, so we
-- want to mimic their naming to avoid conflicts in any
-- subsequent upgrades.

create index if not exists "in_programinstance_trackedentityinstanceid"
    on programinstance (trackedentityinstanceid);
create index if not exists "in_programinstance_programid"
    on programinstance using btree (programid);

create index if not exists "in_programstageinstance_status_executiondate"
    on programstageinstance using btree (status,executiondate);

create index if not exists "in_relationshipitem_trackedentityinstanceid"
    on relationshipitem (trackedentityinstanceid);
create index if not exists "in_relationshipitem_programinstanceid"
    on relationshipitem (programinstanceid);
create index if not exists "in_relationshipitem_programstageinstanceid"
    on relationshipitem (programstageinstanceid);

create unique index if not exists "in_unique_trackedentityprogramowner_teiid_programid_ouid"
    on trackedentityprogramowner using btree (trackedentityinstanceid, programid, organisationunitid);
create index if not exists "in_trackedentityprogramowner_program_orgunit"
    on trackedentityprogramowner (programid, organisationunitid);

create unique index if not exists "in_trackedentityinstance_trackedentityattribute_value"
    on trackedentityattributevalue using btree (trackedentityinstanceid, trackedentityattributeid, lower(value));


-- These indexes are not in the official DHIS release, but we think they
-- improve performance.  We're trying to conform to the DHIS naming standard
-- here in hopes of avoiding future conflicts/duplicates.

create index if not exists "in_relationshipitem_relationshipid"
    on relationshipitem (relationshipid);

create index if not exists "in_usergroupmembers_userid"
    on usergroupmembers (userid);
