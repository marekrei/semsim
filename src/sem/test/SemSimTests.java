package sem.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import sem.test.model.ModelTest;
import sem.test.sim.SimilarityTest;
import sem.test.util.IndexTest;
import sem.test.util.TensorTest;

@RunWith(Suite.class)
@SuiteClasses({ ModelTest.class, 
				SimilarityTest.class,
				IndexTest.class,
				TensorTest.class
				})

public class SemSimTests {

}
