-- These indexes were added in future commits to 2.35, so we
-- wanted to mimic their naming to avoid conflicts in any
-- subsequent upgrades. BUT they turned out to be harmful,
-- so we're dropping them like 3rd period latin. :)

drop index if exists "in_trackedentityinstance_trackedentityattribute_value";


-- These indexes are not in the official DHIS release, but we think they
-- improve performance.  We're trying to conform to the DHIS naming standard
-- here in hopes of avoiding future conflicts/duplicates.

create index if not exists "in_trackedentityinstance_tetid_deleted"
    on trackedentityinstance (trackedentitytypeid, deleted);
