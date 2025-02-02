package org.hisp.dhis.feedback;

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

/**
 * Error series:
 *
 * E2000 - E2999: Data
 * E3000 - E3999: Security
 * E4000 - E4999: Metadata validation
 * E5000 - E5999: Preheat
 * E6000 - E6999: Metadata import
 * E7000 - E7099: Scheduling
 * E7100 - E7199: Aggregate analytics
 * E7200 - E7299: Event analytics
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum ErrorCode
{
    /* Miscellaneous */
    E1000( "API query must be specified" ),
    E1001( "API query contains an illegal string" ),
    E1002( "API version is invalid" ),

    /* Data */
    E2000( "Query parameters cannot be null" ),
    E2001( "At least one data element, data set or data element group must be specified" ),
    E2002( "At least one period, start/end dates, last updated or last updated duration must be specified" ),
    E2003( "Both periods and start/end date cannot be specified" ),
    E2004( "Start date must be before end date" ),
    E2005( "Duration is not valid: `{0}`" ),
    E2006( "At least one organisation unit or organisation unit group must be specified" ),
    E2007( "Organisation unit children cannot be included for organisation unit groups" ),
    E2008( "At least one organisation unit must be specified when children are included" ),
    E2009( "Limit cannot be less than zero: `{0}`" ),
    E2010( "User is not allowed to read data for data set: `{0}`" ),
    E2011( "User is not allowed to read data for attribute option combo: `{0}`" ),
    E2012( "User is not allowed to view org unit: `{0}`" ),
    E2013( "At least one data set must be specified" ),
    E2014( "Unable to parse filter `{0}`" ),
    E2015( "Unable to parse order param: `{0}`" ),
    E2016( "Unable to parse element `{0}` on filter `{1}`. The values available are: {2}" ),

    /* Security */
    E3000( "User `{0}` is not allowed to create objects of type {1}." ),
    E3001( "User `{0}` is not allowed to update object `{1}`." ),
    E3002( "User `{0}` is not allowed to delete object `{1}`." ),
    E3003( "User `{0}` is not allowed to grant users access to user role `{1}`." ),
    E3004( "User `{0}` is not allowed to grant users access to user groups." ),
    E3005( "User `{0}` is not allowed to grant users access to user group `{1}`." ),
    E3006( "User `{0}` is not allowed to externalize objects of type `{1}`." ),
    E3008( "User `{0}` is not allowed to make public objects of type `{1}`." ),
    E3009( "User `{0}` is not allowed to make private objects of type `{1}`." ),
    E3010( "Invalid access string `{0}`." ),
    E3011( "Data sharing is not enabled for type `{0}`, but one or more access strings contains data sharing read or write." ),
    E3012( "User `{0}` does not have read access for object {1}." ),

    /* Metadata Validation */
    E4000( "Missing required property `{0}`." ),
    E4001( "Maximum length of property `{0}`is {1}, but given length was {2}." ),
    E4002( "Allowed length range for property `{0}` is [{1} to {2}], but given length was {3}." ),
    E4003( "Property `{0}` requires a valid email address, was given `{1}`." ),
    E4004( "Property `{0}` requires a valid URL, was given `{1}`." ),
    E4005( "Property `{0}` requires a valid password, was given `{1}`." ),
    E4006( "Property `{0}` requires a valid HEX color, was given `{1}`." ),
    E4007( "Allowed size range for collection property `{0}` is [{1} to {2}], but size given was {3}." ),
    E4008( "Allowed range for numeric property `{0}` is [{1} to {2}], but number given was {3}." ),
    E4009( "Attribute `{0}` is unique, and value `{1}` already exist." ),
    E4010( "Attribute `{0}` is not supported for type `{1}`." ),
    E4011( "Attribute `{0}` is required, but no value was found." ),
    E4012( "Attribute `{0}` contains elements of different period type than the data set it was added to" ),
    E4013( "Invalid Closing date `{0}`, must be after Opening date `{1}`" ),
    E4014( "Invalid UID `{0}` for property `{1}`" ),
    E4015( "Property `{0}` refers to an object that does not exist, could not find `{1}`" ),
    E4016( "Object referenced by the `{0}` property is already associated with another object, value: `{1}`" ),
    E4017( "RenderingType `{0}` is not supported for ValueType `{1}`" ),
    E4018( "Property `{0}` must be set when property `{1}` is `{2}`" ),
    E4019( "Failed to parse pattern `{0}`. {1}" ),
    E4020( "The value `{0}` does not conform to the attribute pattern `{1}`" ),
    E4021( "ID-pattern is required to have 1 generated segment (RANDOM or SEQUENTIAL)." ),
    E4022( "Pattern `{0}` does not conform to the value type `{1}`." ),
    E4023( "Property `{0}` can not be set when property `{1}` is `{2}`. " ),
    E4024( "Property `{0}` must be set when property `{1}` is `{2}`. " ),
    E4025( "Properties `{0}` and `{1}` are mutually exclusive and cannot be used together." ),
    E4026( "One of the properties `{0}` and `{1}` is required when property `{2}` is `{3}`." ),
    E4027( "Value `{0}` is not a valid for property `{1}`" ),
    E4028( "Option set `{0}` already contains option `{1}`" ),
    E4029( "Job parameters cannot be null for job type: {0}" ),
    E4030( "Object could not be deleted because it is associated with another object: {0}" ),

    /* SQL views */
    E4300( "SQL query is null" ),
    E4301( "SQL query must be a select query" ),
    E4302( "SQL query can only contain a single semi-colon at the end of the query" ),
    E4303( "Variables contain null key" ),
    E4304( "Variables contain null value" ),
    E4305( "Variable params are invalid: `{0}`" ),
    E4306( "Variables are invalid: `{0}`" ),
    E4307( "SQL query contains variables not provided in request: `{0}`" ),
    E4308( "Criteria params are invalid: `{0}`" ),
    E4309( "Criteria values are invalid: `{0}`" ),
    E4310( "SQL query contains references to protected tables" ),
    E4311( "SQL query contains illegal keywords" ),

    /* Preheat */
    E5000( "Found matching object for given reference, but import mode is CREATE. Identifier was {0}, and object was {1}." ),
    E5001( "No matching object for given reference. Identifier was {0}, and object was {1}." ),
    E5002( "Invalid reference {0} on object {1} for association `{2}`." ),
    E5003( "Property `{0}` with value `{1}` on object {2} already exists on object {3}." ),
    E5004( "Id `{0}` for type `{1}` exists on more than 1 object in the payload, removing all but the first found." ),

    /* Metadata import */
    E6000( "Program `{0}` has more than one Program Instances"),
    E6001( "ProgramStage `{0}` has invalid next event scheduling property `{1}`. This property need to be data element of value type date and belong the program stage."),

    /* File resource */
    E6100( "Filename not present" ),
    E6101( "File type not allowed" ),

    /* Users */
    E6200( "Feedback message recipients user group not defined" ),

    /* Scheduling */
    E7000( "Failed to add/update job configuration, another job of the same job type is already scheduled with this cron expression: `{0}`" ),
    E7002( "Failed to add/update job configuration, UID does not exist" ),
    E7003( "Failed to add/update job configuration, only interval can be configured for non configurable job type: `{0}`" ),
    E7004( "Failed to add/update job configuration, cron expression must be not null for job with scheduling type CRON: `{0}`" ),
    E7005( "Failed to add/update job configuration, cron expression is invalid: `{0}` " ),
    E7006( "Failed to execute job `{0}`." ),
    E7007( "Failed to add/update job configuration - Delay must be not null for jobs with scheduling type FIXED_DELAY: `{0}`" ),
    E7010( "Failed to validate job runtime - `{0}`" ),

    /* Aggregete analytics */
    E7100( "Query parameters cannot be null" ),
    E7101( "At least one dimension must be specified" ),
    E7102( "At least one data dimension item or data element group set dimension item must be specified" ),
    E7103( "Dimensions cannot be specified as dimension and filter simultaneously: `{0}`" ),
    E7104( "At least one period as dimension or filter, or start and dates, must be specified" ),
    E7105( "Periods and start and end dates cannot be specified simultaneously" ),
    E7106( "Start date cannot be after end date" ),
    E7107( "Start and end dates cannot be specified for reporting rates" ),
    E7108( "Only a single indicator can be specified as filter" ),
    E7109( "Only a single reporting rate can be specified as filter" ),
    E7110( "Category option combos cannot be specified as filter" ),
    E7111( "Dimensions cannot be specified more than once: `{0}`" ),
    E7112( "Reporting rates can only be specified together with dimensions of type: `{0}`" ),
    E7113( "Assigned categories cannot be specified when data elements are not specified" ),
    E7114( "Assigned categories can only be specified together with data elements, not indicators or reporting rates" ),
    E7115( "Data elements must be of a value and aggregation type that allow aggregation: `{0}`" ),
    E7116( "Indicator expressions cannot contain cyclic references: `{0}`" ),
    E7117( "A data dimension 'dx' must be specified when output format is DATA_VALUE_SET" ),
    E7118( "A period dimension 'pe' must be specified when output format is DATA_VALUE_SET" ),
    E7119( "An organisation unit dimension 'ou' must be specified when output format is DATA_VALUE_SET" ),
    E7120( "User: `{0}` is not allowed to view org unit: `{1}`" ),
    E7121( "User: `{0}` is not allowed to read data for `{1}`: `{2}`"),
    E7122( "Data approval level does not exist: `{0}`" ),
    E7123( "Current user is constrained by a dimension but has access to no dimension items: `{0}`" ),
    E7124( "Dimension is present in query without any valid dimension options: `{0}`" ),
    E7125( "Dimension identifier does not reference any dimension: `{0}`" ),
    E7126( "Column must be present as dimension in query: `{0}`" ),
    E7127( "Row must be present as dimension in query: `{0}`" ),
    E7128( "Query result set exceeded max limit: `{0}`" ),
    E7129( "Program is specified but does not exist: `{0}`" ),
    E7130( "Program stage is specified but does not exist: `{0}`" ),
    E7131( "Query failed, likely because the query timed out" ),
    E7132( "Query failed. An Indicator expression is resulting in invalid operation: division by zero" ),
    E7133( "This query cannot be executed, possibly because of invalid types or invalid operation" ),

    /* Event analytics */
    E7200( "At least one organisation unit must be specified" ),
    E7201( "Dimensions cannot be specified more than once: `{0}`" ),
    E7202( "Query items cannot be specified more than once: `{0}`" ),
    E7203( "Value dimension cannot also be specified as an item or item filter" ),
    E7204( "Value dimension or aggregate data must be specified when aggregation type is specified" ),
    E7205( "Start and end date or at least one period must be specified" ),
    E7206( "Start date is after end date: `{0}`, `{1}`" ),
    E7207( "Page number must be a positive number: `{0}`" ),
    E7208( "Page size must be zero or a positive number: `{0}`" ),
    E7209( "Limit is larger than max limit: `{0}`, `{1}`" ),
    E7210( "Time field is invalid: `{0}`" ),
    E7211( "Org unit field is invalid: `{0}`" ),
    E7212( "Cluster size must be a positive number: `{0}`" ),
    E7213( "Bbox is invalid, must be on format: 'min-lng,min-lat,max-lng,max-lat': `{0}`" ),
    E7214( "Cluster field must be specified when bbox or cluster size are specified" ),
    E7215( "Query item cannot specify both legend set and option set: `{0}`" ),
    E7216( "Query item must be aggregateable when used in aggregate query: `{0}`" ),
    E7217( "User is not allowed to view event analytics data: `{0}`" ),
    E7218( "Spatial database support is not enabled" ),
    E7219( "Data element must be of value type coordinate or org unit in order to be used as coordinate field: `{0}`" ),
    E7220( "Attribute must be of value type coordinate or org unit in order to be used as coordinate field: `{0}`" ),
    E7221( "Coordinate field is invalid: `{0}`" ),
    E7222( "Query item or filter is invalid: `{0}`" ),
    E7223( "Value does not refer to a data element or attribute which are numeric and part of the program: `{0}`" ),
    E7224( "Item identifier does not reference any data element, attribute or indicator part of the program: `{0}`" ),
    E7225( "Program stage is mandatory for data element dimensions in enrollment analytics queries: `{0}`" ),
    E7226( "Dimension is not a valid query item: `{0}`" ),
    E7227( "Relationship entity type not supported: `{0}`" ),

    /* Org unit analytics */
    E7300( "At least one organisation unit must be specified" ),
    E7301( "At least one organisation unit group set must be specified" ),

    /* Debug analytics */
    E7400( "Debug query must contain at least one data element, one period and one organisation unit" );

    private String message;

    ErrorCode( String message )
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }
}
