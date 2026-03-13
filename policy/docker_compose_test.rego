package main

import rego.v1

test_restart_unless_stopped_passes if {
	count(violation_restart_policy) == 0 with input as {"services": {"web": {"restart": "unless-stopped"}}}
}

test_missing_restart_fails if {
	count(violation_restart_policy) > 0 with input as {"services": {"web": {"image": "nginx"}}}
}

test_wrong_restart_fails if {
	count(violation_restart_policy) > 0 with input as {"services": {"web": {"restart": "always"}}}
}

test_multiple_services_all_pass if {
	count(violation_restart_policy) == 0 with input as {"services": {
		"web": {"restart": "unless-stopped"},
		"db": {"restart": "unless-stopped"},
	}}
}

test_multiple_services_one_fails if {
	count(violation_restart_policy) == 1 with input as {"services": {
		"web": {"restart": "unless-stopped"},
		"db": {"restart": "always"},
	}}
}
