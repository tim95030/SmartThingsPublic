/**
 *  Copyright 2017 Tim Schroeder
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Button Controller for Lights with hue scene capability
 *
 *  Author: Tim Schroeder
 *  Date: 2017-03-04
 */
definition(
    name: "Button Controller For Lights",
    namespace: "tim95030",
    author: "Tim Schroeder",
    description: "Control lights with buttons. Supports Hue Scenes",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
  page(name: "selectButton")
  page(name: "configureButton1")
  page(name: "configureButton2")
  page(name: "configureButton3")
  page(name: "configureButton4")
}

def selectButton() {
  dynamicPage(name: "selectButton", title: "First, select your button device", nextPage: "configureButton1", uninstall: configured()) {
    section {
      input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true
    }
  }
}

def configureButton1() {
  dynamicPage(name: "configureButton1", title: "Now let's decide how to use the first button",
    nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))
}
def configureButton2() {
  dynamicPage(name: "configureButton2", title: "If you have a second button, set it up here",
    nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}

def configureButton3() {
  dynamicPage(name: "configureButton3", title: "If you have a third button, you can do even more here",
    nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
  dynamicPage(name: "configureButton4", title: "If you have a fourth button, you rule, and can set it up here",
    install: true, uninstall: true, getButtonSections(4))
}

def getButtonSections(buttonNumber) {
  return {
    section("Hue Lights") {
      input "lights_${buttonNumber}_pushed", "capability.switch", title: "Lights", multiple: true, required: false
      input "lightsAction_${buttonNumber}_pushed", "enum", multiple: false, title: "Button Action:", required: false, metadata:[values:["On","Off"]]
      if (buttonNumber == 1) {
        getColorLightInputs(buttonNumber)
      } else {
        input "lightsLevel_${buttonNumber}_pushed", "integer", title: "Level: 1 to 100", defaultValue: 100, required: true, multiple: false
        input "lightsColor_${buttonNumber}_pushed", "enum", multiple: false, title: "Color (Default is Soft White):", defaultValue: "Soft White", required: true, metadata:[values:["Soft White","White","Daylight","Warm White","Red","Green","Blue","Yellow","Orange","Purple","Pink"]]
      }
    }
    section("Dimmer Lights") {
      input "dimmers_${buttonNumber}_pushed", "capability.switch", title: "Dimmers", multiple: true, required: false
      input "dimmersAction_${buttonNumber}_pushed", "enum", multiple: false, title: "Button Action:", required: false, metadata:[values:["On","Off"]]
      if (buttonNumber == 1) {
        getDimmerLightsInputs(buttonNumber)
      } else {
        input "dimmersLevel_${buttonNumber}_pushed", "number", title: "Level: 1 to 99", defaultValue: 99, required: true, multiple: false
      }
    }
  }
}

def getColorLightInputs(buttonNumber) {
  5.times {
    input "lightsLevel${it}_${buttonNumber}_pushed", "integer", title: "Level: 1 to 100", defaultValue: 100, required: true, multiple: false
    input "lightsColor${it}_${buttonNumber}_pushed", "enum", multiple: false, title: "Color (Default is Soft White):", defaultValue: "Soft White", required: true, metadata:[values:["Soft White","White","Daylight","Warm White","Red","Green","Blue","Yellow","Orange","Purple","Pink"]]
  }
}

def getDimmerLightsInputs(buttonNumber) {
  5.times {
    input "dimmersLevel${it}_${buttonNumber}_pushed", "number", title: "Level: 1 to 99", defaultValue: 99, required: true, multiple: false
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(buttonDevice, "button", buttonEvent)
}

def configured() {
  return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4)
}

def buttonConfigured(idx) {
  return settings["lights_$idx_pushed"] || settings["dimmer_$idx_pushed"]
}

def buttonEvent(evt){
  if(allOk) {
    def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
    def value = evt.value
    log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
    log.debug "button: $buttonNumber, value: $value"

    def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
    log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"

    if(recentEvents.size <= 1){
      switch(buttonNumber) {
        case ~/.*1.*/:
          executeHandlers(1, value)
          break
        case ~/.*2.*/:
          executeHandlers(2, value)
          break
        case ~/.*3.*/:
          executeHandlers(3, value)
          break
        case ~/.*4.*/:
          executeHandlers(4, value)
          break
      }
    } else {
      log.debug "Found recent button press events for $buttonNumber with value $value"
    }
  }
}

def executeHandlers(buttonNumber, value) {
  log.debug "executeHandlers: $buttonNumber - $value"
  def currentState = ""

  if (!state.clickNumber || state.clickNumber > 4) {
    state.clickNumber = 0
  }

  if (buttonNumber == 1) {
    state.clickNumber = state.clickNumber + 1
    currentState = "${state.clickNumber}"
  }

  log.debug "state.clickNumber: ${state.clickNumber}"

  def lights = find('lights', buttonNumber, value)
  def lightsAction = find('lightsAction', buttonNumber, value)
  def lightsLevel = find("lightsLevel${currentState}", buttonNumber, value)
  def lightsColor = find("lightsColor${currentState}", buttonNumber, value)
  if (lights != null) toggle(lights, lightsAction, lightsLevel, lightsColor)

  def dimmers = find('dimmers', buttonNumber, value)
  def dimmersAction = find('dimmersAction', buttonNumber, value)
  def dimmersLevel = find("dimmersLevel${currentState}", buttonNumber, value)
  if (dimmers != null) toggle(dimmers, dimmersAction, dimmersLevel)
}

def find(type, buttonNumber, value) {
  def preferenceName = type + "_" + buttonNumber + "_" + value
  def pref = settings[preferenceName]
  if(pref != null) {
    log.debug "Found: $pref for $preferenceName"
  }

  return pref
}

def findMsg(type, buttonNumber) {
  def preferenceName = type + "_" + buttonNumber
  def pref = settings[preferenceName]
  if(pref != null) {
    log.debug "Found: $pref for $preferenceName"
  }

  return pref
}

def toggle(devices, action, level, color = null) {
  log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

  setLightProperties(devices, level, color)

  if (action == "On") {
    devices.on()
  }

  if (action == "Off") {
    devices.off()
  }
}

def setColor(devices, level, color) {
  state.newValue = null
  if(color) {
    switch(color) {
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

    def myLightLevel = level as Integer
    def myhueColor = state.hueColor1 as Integer
    def mySat = state.saturation1 as Integer
    state.newValue = [hue: myhueColor, saturation: mySat, level: myLightLevel]
    devices*.setColor(state.newValue)
  } else {
    if (level > 99) {
      state.level = 99
    } else {
      state.level = level
    }
    def newLevel = state.level as Integer
    devices*.setLevel(newLevel)
  }
}
