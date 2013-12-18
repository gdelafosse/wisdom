package org.wisdom.test;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.runner.Description;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.test.internals.ChameleonExecutor;
import org.wisdom.test.shared.InVivoRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The Wisdom Test Runner.
 */
public class WisdomRunner extends BlockJUnit4ClassRunner implements Filterable, Sortable {


    private static Logger LOGGER = LoggerFactory.getLogger(WisdomRunner.class);
    private final ChameleonExecutor executor;
    private final InVivoRunner delegate;
    private final File basedir;
    private File backupRuntime;

    public WisdomRunner(Class<?> klass) throws Exception {
        super(klass);
        basedir = checkWisdomInstallation();
        File bundle = detectApplicationBundleIfExist(new File(basedir, "application"));
        if (bundle != null && bundle.exists()) {
            LOGGER.info("Application bundle found in the application directory (" + bundle.getAbsoluteFile());
        }
        bundle = detectApplicationBundleIfExist(new File(basedir, "runtime"));
        if (bundle != null && bundle.exists()) {
            LOGGER.info("Application bundle found in the runtime directory (" + bundle.getAbsoluteFile());
        }

        System.setProperty("application.configuration",
                new File(basedir, "/conf/application.conf").getAbsolutePath());
        executor = ChameleonExecutor.instance(basedir);
        executor.deployProbe();

        delegate = executor.getInVivoRunnerInstance(klass);
    }

    /**
     * Detects if the bundle is present in the given directory.
     * The detection stops when a jar file contains a class file from target/classes and where sizes are equals.
     *
     * @param directory the directory to analyze.
     * @return the bundle file if detected.
     * @throws IOException cannot open files.
     */
    private File detectApplicationBundleIfExist(File directory) throws IOException {
        if (!directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        // Find one entry from classes.
        final File classes = new File("target/classes");
        Collection<File> clazzes = FileUtils.listFiles(classes, new String[]{"class"}, true);
        // Transform into classnames but using / and not . as package separator.
        Collection<String> classnames = Collections2.transform(clazzes, new Function<File, String>() {
            @Override
            public String apply(File input) {
                String absolute = input.getAbsolutePath();
                return absolute.substring(classes.getAbsolutePath().length() + 1);
            }
        });

        // Iterate over the set of jar files.
        for (File file : files) {
            if (!file.getName().endsWith("jar")) {
                continue;
            }

            JarFile jar = null;
            try {
                jar = new JarFile(file);
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        if (classnames.contains(entry.getName())) {
                            // Found !
                            return file;
                        }
                    }
                }
            } finally {
                IOUtils.closeQuietly(jar);
            }

        }

        return null;


    }

    private File checkWisdomInstallation() {
        File directory = new File("target/wisdom");
        if (!directory.isDirectory()) {
            throw new ExceptionInInitializerError("Wisdom is not installed in " + directory.getAbsolutePath() + " - " +
                    "please check your execution directory, and that Wisdom is prepared correctly. To setup Wisdom, " +
                    "run 'mvn pre-integration-test' from your application directory");
        }
        File conf = new File(directory, "conf/application.conf");
        if (!conf.isFile()) {
            throw new ExceptionInInitializerError("Wisdom is not correctly installed in " + directory.getAbsolutePath()
                    + " - the configuration file does not exist. Please check your Wisdom runtime. To setup Wisdom, " +
                    "run 'mvn clean pre-integration-test' from your application directory");
        }
        return directory;
    }

    @Override
    protected Object createTest() throws Exception {
        return delegate.createTest();
    }

    @Override
    public void run(RunNotifier notifier) {
        delegate.run(notifier);
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        delegate.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        delegate.sort(sorter);
    }
}
