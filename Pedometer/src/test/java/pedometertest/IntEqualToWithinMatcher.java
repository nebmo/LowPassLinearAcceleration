package pedometertest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

public class IntEqualToWithinMatcher {
    private final Integer mValue;

    public IntEqualToWithinMatcher(Integer value) {
        mValue = value;
    }

    public static IntEqualToWithinMatcher equalTo(Integer value) {
        return new IntEqualToWithinMatcher(value);
    }

    public Matcher<Integer> within(int tolerance) {
        return new WithinMatcher(mValue, tolerance);
    }

    public class WithinMatcher extends TypeSafeMatcher<Integer> {
        private final int mTolerance;
        private final Integer mValue;

        public WithinMatcher(Integer value, int tolerance) {
            mValue = value;
            mTolerance = tolerance;
        }

        @Override
        public boolean matchesSafely(Integer actual) {
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
