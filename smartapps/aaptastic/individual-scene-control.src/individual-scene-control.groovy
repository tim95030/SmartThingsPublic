/**
 * Individual Scene Creation & Control - for Hues, Dimmers, and/or Switches
 * This App can be used separately from, or in conjunction with, my "Whole House Scene Creation & Control" App.
 *
 *
 *  Version 1.0.0 (2015-1-7)
 *
 *  The latest version of this file can be found at:
 *  <TBD>
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2015 Anthony Pastor
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

 definition(
    name: "Individual Scene Control",
    namespace: "Aaptastic",
    author: "Anthony Pastor",
    description: "This app sets lighting levels for a group of HUES, Dimmers, & Switches to specific colors / levels.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page(name: "mainPage", title: "Scene Inputs:", install: true, uninstall: true)
  page(name: "timeIntervalInput", title: "Only during a certain time") {
    section {
      input "starting", "time", title: "Starting", required: false
      input "ending", "time", title: "Ending", required: false
    }
  }
}


def mainPage() {
  dynamicPage(name: "mainPage") {
    section("Select (Virtual) Presence Device:"){
      input "someoneHome", "capability.switch", multiple: false, required: true
    }
      section("Select Triggers:", hideWhenEmpty: true){
      input "motions", "capability.motionSensor", title: "Motion Sensor(s)", multiple: true, required: false
      input "contacts", "capability.contactSensor", title: "Contact Sensor(s)", multiple: true, required: false
    }
    section("HUE Lights", hideWhenEmpty: true) {
      input "group1", "capability.colorControl", title: "Select:", multiple: true, required: false
        input ("group1Lvl", "integer", title: "Level (default is 100):", required: false, multiple: false)
          input ("group1Color", "enum", multiple: false, title: "Color (Default is Soft White):", required: false, metadata:[values:["Soft White","White","Daylight","Warm White","Red","Green","Blue","Yellow","Orange","Purple","Pink"]])
      }

    section("Dimmers", hideWhenEmpty: true) {
      input "group2", "capability.switchLevel", title: "Select:", multiple: true, required: false
          input ("group2Level", "number", title: "Level: 1 to 99(default)", required: false, multiple: false)
      }

      section("Switches ON..."){
      input "switch1", "capability.switch", multiple: true, required: false
    }
      section("Switches OFF..."){
      input "switch2", "capability.switch", multiple: true, required: false
    }
      section("And then turn ALL OF THE ABOVE OFF..."){
      input "delayMinutes", "number", title: "When there's been no motion for ___ Minutes?"
    }


    section("Optional Time Interval", hideable: true, hidden: true) {
      href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
    }

    section([mobileOnly:true]) {
      mode title: "Set for specific mode(s)", required: false
            label title: "Assign a name", required: false
        }
  }
}

def installed()
{
  state.someoneHome = false

    initialize()

}

def updated() {

  unsubscribe()
    unschedule

    initialize()

}

def initialize() {

    subscribe(app, appTouch)
  subscribe(someoneHome, "switch", switchHome)


    if (motions) {
      subscribe(motions, "motion", motionHandler)
    }
    if (contacts) {
      subscribe(contacts, "contact", contactHandler)
    }
  subscribe(group1, "level", switchChange)
    subscribe(group2, "level", switchChange)


    colorSelection()
    checkHome()
    schedule("0 * * * * ?", "scheduleCheck")
}


def checkHome() {

    if (someoneHome.currentValue("switch") == "on") {
      log.debug "checkHome: state someoneHome is now ON."
        state.someoneHome = true
    } else {
      log.debug "checkHome: state someoneHome is now OFF."
        state.someoneHome = false
    }
}

def switchHome(evt) {
  log.debug "switchHome: $evt.name: $evt.value"
    if (evt.value == "on") {
    state.someoneHome = true
    } else {
      state.someoneHome = false
    }
}

def colorSelection() {
  state.newValue = null

  state.lightLevel1 = group1Lvl ?: 100
  state.hueColor1 = 23
  state.saturation1 = 56

  switch(group1Color) {
    case "White":
      state.hueColor1 = 52
      state.saturation1 = 19
      break;
    case "Daylight":
      state.hueColor1 = 53
      state.saturation1 = 91
      break;
    case "Soft White":
      state.hueColor1 = 23
      state.saturation1 = 56
      break;
    case "Warm White":
      state.hueColor1 = 20
      state.saturation1 = 80 //83
      break;
    case "Blue":
      state.hueColor1 = 70
      state.saturation1 = 100
            break;
    case "Green":
      state.hueColor1 = 39
      state.saturation1 = 100
            break;
    case "Yellow":
      state.hueColor1 = 25
      state.saturation1 = 100
            break;
    case "Orange":
      state.hueColor1 = 10
      state.saturation1 = 100
            break;
    case "Purple":
      state.hueColor1 = 75
      state.saturation1 = 100
            break;
    case "Pink":
      state.hueColor1 = 83
      state.saturation1 = 100
            break;
    case "Red":
      state.hueColor1 = 100
      state.saturation1 = 100
      break;
  }

  if (group1) {
      def myLightLevel = state.lightLevel1 as Integer
    def myhueColor = state.hueColor1 as Integer
      def mySat = state.saturation1 as Integer
    state.newValue = [hue: myhueColor, saturation: mySat, level: myLightLevel]
    }
    if (group2) {
      if (group2Level > 99 || !group2Level ) {
        state.group2Lvl = 99
      } else {
        state.group2Lvl = group2Level
      }
  }
}

def switchChange(evt) {
  log.debug "SwitchChange: $evt.name: $evt.value"
  state.changeValue = evt.value as Integer

  if (!state.someoneHome) {
    log.debug "SwitchChange: No one's home...."
        if(state.changeValue > 0 ) {
          log.info "...but new light level > 0, so setting scheduleCheck."
          unschedule
      schedule("0 * * * * ?", "scheduleCheck")
    }
  }
}

def appTouch(evt) {

  log.info "appTouch evt.value is ${evt.value}."

      if (group1) {
        group1*.setColor(state.newValue)
    }

      if (group2){
            def myLevel = state.group2Lvl as Integer
        group2*.setLevel(myLevel)
      }

    if (switch1) {
        switch1*.on()
      }

      if (switch2) {
        switch2*.off()
      }

    state.inactiveAt = null
    unschedule
    schedule("0 * * * * ?", "scheduleCheck")

}


def contactHandler(evt) {
  log.debug "contactHandler: $evt.name: $evt.value"

  if (state.someoneHome) {
    log.debug "contactHandler: Someone's home, so run..."
      if (evt.value == "open") {
      def myLevel = null
        if (group1) {
          group1*.setColor(state.newValue)
      }

        if (group2){
              myLevel = state.group2Lvl as Integer
          group2*.setLevel(myLevel)
        }

      if (switch1) {
          switch1*.on()
        }

        if (switch2) {
          switch2*.off()
        }

            state.inactiveAt = null


      } else if (evt.value == "closed") {

          state.inactiveAt = now()
        unschedule
        schedule("0 * * * * ?", "scheduleCheck")

      }

  } else {
    log.debug "contactHandler: No one's home, so don't run..."
  }
}

def motionHandler(evt) {
  log.debug "motionHandler: $evt.name: $evt.value"

  if (state.someoneHome) {
    log.debug "motionHandler: Someone's home, so run..."

      if (evt.value == "active") {
          log.info "Motion..."
          state.inactiveAt = null
            def myLevel = null

          if (group1) {
        log.info "Turning on Group1 lights..."
              group1*.setColor(state.newValue)
            }

          if (group2) {
        log.info "Turning on Group2 lights..."

                myLevel = state.group2Lvl as Integer
                group2*.setLevel(myLevel)

        }

      if (switch1) {
              log.info "Turning on Switch1 switches..."
          switch1*.on()
      }

      } else {
          log.info "No motion - scheduling ScheduleCheck."
          unschedule()
        state.inactiveAt = now()
        schedule("0 * * * * ?", "scheduleCheck")
      }

  } else {
    log.info "motionHandler: No one's home, so don't run..."
  }
}



def scheduleCheck() {
  log.info "schedule check, ts = ${state.inactiveAt}"

  if(state.inactiveAt != null) {
        def elapsed = now() - state.inactiveAt
        log.info "${elapsed / 1000} sec since motion stopped"
        def threshold = 1000 * 60 * delayMinutes

    if (elapsed >= threshold) {
          log.debug "Turning off lights"
        if (group1) {
            group1*.off()
        }
          if (group2) {
              group2*.off()
        }
          if (switch1) {
        switch1*.off()
          }

          state.inactiveAt = null
      }
    }
}




private hhmm(time, fmt = "h:mm a")
{
  def t = timeToday(time, location.timeZone)
  def f = new java.text.SimpleDateFormat(fmt)
  f.setTimeZone(location.timeZone ?: timeZone(time))
  f.format(t)
}

private timeIntervalLabel()
{
  (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
