#!/usr/bin/env python3

from gpiozero import OutputDevice
from time import sleep

# BCM GPIO number, not physical pin number.
# Change this if the relay/MOSFET is wired to another GPIO.
RESET_GPIO = 17

# active_high=True means GPIO HIGH activates the relay/reset circuit.
# If the relay is active-low, change active_high=False.
relay = OutputDevice(RESET_GPIO, active_high=True, initial_value=False)

print("RESET_START")

relay.on()
sleep(1)
relay.off()

print("RESET_DONE")
