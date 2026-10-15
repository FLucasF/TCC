"""The acceptance of each change request: what that scenario's text asks for, checked through the API. Only the
scenarios written so far are here; a scenario without checks yet runs the regression alone.
"""

import acceptance_01_rate_plans
from checks import Check

ACCEPTANCE_BY_SCENARIO: dict[str, list[Check]] = {
    "01": acceptance_01_rate_plans.CHECKS,
}


def checks_for(scenario: str) -> list[Check]:
    return ACCEPTANCE_BY_SCENARIO.get(scenario, [])
