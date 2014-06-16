package pedometertest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

/**
 * Created by david.laurell on 2014-02-20.
 */
public class EqualToWithinMatcher {
    private final Double mValue;

    public EqualToWithinMatcher(Double value) {
        mValue = value;
    }

    public static EqualToWithinMatcher equalTo(Double value) {
        return new EqualToWithinMatcher(value);
    }

    public Matcher<Double> within(double tolerance) {
        return new WithinMatcher(mValue, tolerance);
    }

    public class WithinMatcher extends TypeSafeMatcher<Double> {
        private final double mTolerance;
        private final Double mValue;

        public WithinMatcher(Double value, double tolerance) {
            mValue = value;
            mTolerance = tolerance;
        }

        @Override
        public boolean matchesSafely(Double actual) {
            double min = actual - mTolerance;
            double max = actual + mTolerance;

            return mValue >= min && mValue <= max;
        }

        @Override
        public void describeTo(Description description) {
            description.appendValue(mValue);
            description.appendText(" +/- ");
            description.appendValue(mTolerance);
        }
    }
}
