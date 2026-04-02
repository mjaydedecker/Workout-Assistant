# Workout-Assistant Project - Manual Testing Finds #1
Findings from the first set of manual testing.

## Issues
The following are issues that are to be corrected:
- Vibrations for Rest and Inactivity alerts are operating correctly but there are no sounds playing for the timers even when all volume sliders were at medium level or higher at the OS level.  When sounds are enabled for alerts they should play.  
- The Workday Name cannot currently be edited.  This is a requirement in the FSD.

## Enhancements
The following are enhancements that are to be implemented:
- Data retention from version to version of the Workout-Assistant application.  All exercise, Workday, exercise session, history and settings are to be retained from version to version and data migrated if necessary due to database changes.
- During an active exercises session, the user can scroll through the exercises they are to before.  However, when doing this the timer display scrolls off the screen when strolling to the bottom of the list.  I would like the timer display to not be part of the screen that scrolls.
- In the workout history, I would like to be able to choose between a list of by exercise sessions or a calendar view of my exercise sessions.  From the list or the calendar view I would like to be able to press on a exercise session and see the details of the session.