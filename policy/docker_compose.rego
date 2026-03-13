package main

import rego.v1

violation_restart_policy contains violation if {
	some name, svc in input.services
	not svc.restart == "unless-stopped"
	violation := {
		"msg": sprintf("service %q must have restart: unless-stopped", [name]),
		"service": name,
	}
}
