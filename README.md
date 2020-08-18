# SAE_Dashboard
Dashboard for Illinois Tech Motorsports
This is the ever improving code for the Illinois Tech Motorsports Dashboard. 
Started in the 2019-2020 season.

# About
The goal of our app is to provide additional safety, controllability, and visual appeal. The dashboard gets all of its information through two Teensy 3.6 microcontrollers. We used the Android Studio development software to create the dashboard app. This app comprises three different tabs: drive-time, data logging, and troubleshooting. 
# Drive-Time Tab
The drive-time tab consists of a speedometer, power gauge, and battery life gauge. This will be the primary tab shown while the car is being driven. There is also space on the main screen for a warning light. This light will appear if a fault goes off in the car. For example, if the motors are overheated then a motor-over-temperature fault will go off. The caution light will then be displayed on the main screen of the dashboard. 
# Data Logging Tab
The data logging tab provides a comprehensive list of all data points from different parts of the car. These data points are then displayed on this tab. These data points are important to assess afterward to see driving patterns and possible ways to correct any abnormal tendencies the car may experience while on the road. These data points are important enough to record, but not necessarily important enough to be displayed on the primary tab. For example, active aero wing angle, motor temperature, etc. All data points will then be exported to a CSV file where it can be sent wirelessly to a spreadsheet online. 
# Troubleshooting Tab
The final tab is the troubleshooting tab. This tab displays our car from a birds-eye view. Any part of the car can be clicked on and a corresponding troubleshooting guide will appear for that certain part. For example, if the motors were clicked on then a list of possible faults will show up for that part along with how to fix the issue if necessary. Along with that, in the event that a fault was to go off in the car, not only will a warning light appear in the main tab, but the area of the car where it went off will light up in the car display in the troubleshooting tab. This enables our team to quickly identify any issues and anybody will know how to fix the issue.
