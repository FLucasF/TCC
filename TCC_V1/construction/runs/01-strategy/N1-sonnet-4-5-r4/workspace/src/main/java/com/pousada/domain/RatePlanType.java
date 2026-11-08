package com.pousada.domain;

import com.pousada.domain.rateplan.*;

public enum RatePlanType {
    STANDARD {
        @Override
        public RatePlan create() {
            return new StandardRatePlan();
        }
    },
    NON_REFUNDABLE {
        @Override
        public RatePlan create() {
            return new NonRefundableRatePlan();
        }
    },
    CORPORATE {
        @Override
        public RatePlan create() {
            return new CorporateRatePlan();
        }
    },
    WEEKEND_PACKAGE {
        @Override
        public RatePlan create() {
            return new WeekendPackageRatePlan();
        }
    },
    LONG_STAY {
        @Override
        public RatePlan create() {
            return new LongStayRatePlan();
        }
    };

    public abstract RatePlan create();
}
