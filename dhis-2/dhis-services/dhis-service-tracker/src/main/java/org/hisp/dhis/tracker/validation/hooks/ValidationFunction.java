package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;

/**
 * Helper class to
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@FunctionalInterface
public interface ValidationFunction<T extends TrackerDto>
{
    void validateTrackerDto( T obj, ValidationErrorReporter reportFork );
}
