package com.eventpass.organizer;

public record SuspendOrganizerResponse(
     OrganizerResponse organizer, int unpublishedEventCount
) {

}
