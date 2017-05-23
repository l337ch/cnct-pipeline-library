package zonarsystems.pipeline

import com.lesfurets.jenkins.unit.cps.BasePipelineTestCPS

import jdk.nashorn.internal.runtime.linker.JavaSuperAdapter

import org.junit.Before
import org.junit.Test
import org.junit.Rule

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource
import static org.assertj.core.api.Assertions.assertThat

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.junit.Assert.assertTrue
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters


class ApplicationPipelineTest extends BasePipelineTestCPS {
		
	@Rule
    public TemporaryFolder folder = new TemporaryFolder()

    String sharedLibs = this.class.getResource('./').getFile()
	def scriptDir = getClass().protectionDomain.codeSource.location.path
	

    @Parameter(0)
    public String script
    @Parameter(1)
    public boolean allowOverride
    @Parameter(2)
    public boolean implicit
    @Parameter(3)
    public boolean expected

    @Override
    @Before
    void setUp() throws Exception {
		println "cwd=${scriptDir}"
        scriptRoots += 'src'
        super.setUp()
        binding.setVariable('scm', [branch: 'master'])
	}
		
	@Test
    void library_annotation() throws Exception {
        boolean exception = false
		boolean expectedException = false
        def library = library().name('pipeline')
                        .defaultVersion("master")
                        .allowOverride(allowOverride)
                        .implicit(implicit)
                        .targetPath(sharedLibs)
                        .retriever(localSource(sharedLibs))
                        .build()
        helper.registerSharedLibrary(library)
		
		  
        try {
            def script = loadScript("src/test/jenkins/loadLibrary.jenkins")
            script.execute()
            printCallStack()
        } catch (e) {
            e.printStackTrace()
            exception = true
        }
        assertThat(exception).isEqualTo(expectedException)

	}
	
	@Test
	void test_checkImage() throws Exception {
		boolean exception = false
		boolean expectedException = false
		def library = library().name('pipeline')
						.defaultVersion("master")
						.allowOverride(allowOverride)
						.implicit(implicit)
						.targetPath(sharedLibs)
						.retriever(localSource(sharedLibs))
						.build()
		helper.registerSharedLibrary(library)
		
		  
		try {
			def script = loadScript("src/test/jenkins/testCheckImage.jenkins")
			script.loadLibrary(script)
			printCallStack()
		} catch (e) {
			e.printStackTrace()
			exception = true
		}
		assertThat(exception).isEqualTo(expectedException)

	}
	
}