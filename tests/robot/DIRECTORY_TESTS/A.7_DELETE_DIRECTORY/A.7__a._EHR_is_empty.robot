*** Settings ***
Documentation    Main flow: delete directory on empty EHR
...
...     Preconditions:
...         An EHR with ehr_id exists and doesn't have directory.
...
...     Flow:
...         1. Invoke the delete directory service for the ehr_id
...         2. The service should return an error related to the non existent directory
...
...     Postconditions:
...         None
Metadata        TOP_TEST_SUITE    DIRECTORY
Resource        ${CURDIR}${/}../../_resources/suite_settings.robot

#Suite Setup  startup SUT
# Test Setup  start openehr server
# Test Teardown  restore clean SUT state
#Suite Teardown  shutdown SUT

Force Tags



*** Test Cases ***
Main flow: delete directory on empty EHR

    create EHR
    delete DIRECTORY - fake version_uid (JSON)
    validate DELETE response - 412 precondition failed
