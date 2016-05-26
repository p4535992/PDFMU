/* 
 * Copyright (C) 2016 Hobrasoft s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.hobrasoft.pdfmu;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import cz.hobrasoft.pdfmu.error.ErrorType;
import cz.hobrasoft.pdfmu.jackson.Inspect;
import cz.hobrasoft.pdfmu.operation.OperationException;
import cz.hobrasoft.pdfmu.operation.OperationInspect;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

/**
 * @author Filip Bartek
 */
@RunWith(DataProviderRunner.class)
public class MainTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().mute().enableLog();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().mute().enableLog();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testTooFewArguments() {
        exit.expectSystemExitWithStatus(ErrorType.PARSER_TOO_FEW_ARGUMENTS.getCode());
        Main.main(new String[]{});
        assert false;
    }

    @Test
    public void testVersion() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new Assertion() {
            @Override
            public void checkAssertion() {
                // The terminating line break is introduced by Argparse4j
                Assert.assertEquals(String.format("%1$s\n", Main.getProjectVersion()),
                        systemOutRule.getLogWithNormalizedLineSeparator());
            }
        });
        Main.main(new String[]{"--version"});
        assert false;
    }

    @Test
    public void testHelp() {
        exit.expectSystemExitWithStatus(0);
        Main.main(new String[]{"--help"});
        assert false;
    }

    @Test
    public void testLegalNotice() {
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new Assertion() {
            @Override
            public void checkAssertion() {
                Assert.assertEquals(Main.getLegalNotice(),
                        systemOutRule.getLogWithNormalizedLineSeparator());
            }
        });
        Main.main(new String[]{"--legal-notice"});
        assert false;
    }

    @Ignore
    @Test
    public void testInspect() {
        Assert.fail();
    }

    public enum PdfVersion {
        V12, V13, V14, V15, V16, V17;

        static PdfVersion DEFAULT = V16;

        private char toChar() {
            switch (this) {
                case V12:
                    return '2';
                case V13:
                    return '3';
                case V14:
                    return '4';
                case V15:
                    return '5';
                case V16:
                    return '6';
                case V17:
                    return '7';
            }
            assert false;
            return 0;
        }

        @Override
        public String toString() {
            return String.format("1.%1$c", toChar());
        }

        public String resourceName() {
            return String.format("blank-1%1$c.pdf", toChar());
        }

        public File getFile(TemporaryFolder folder) throws IOException {
            String resourceName = resourceName();
            assert resourceName != null;
            File document = folder.newFile(resourceName);
            assert document.exists();
            ClassLoader classLoader = this.getClass().getClassLoader();
            InputStream in = classLoader.getResourceAsStream(resourceName);
            assert in != null;
            OutputStream out = new FileOutputStream(document);
            assert out != null;
            IOUtils.copy(in, out);
            return document;
        }
    }

    public enum OnlyIfLower {
        No,
        Yes;

        public boolean toBoolean() {
            return this == Yes;
        }
    }

    public enum Force {
        No,
        Yes;

        public boolean toBoolean() {
            return this == Yes;
        }
    }

    public static class UpdateVersionInput {

        public UpdateVersionInput(Force force, PdfVersion inputVersion,
                PdfVersion requestedVersion, OnlyIfLower onlyIfLower) {
            this.force = force;
            this.inputVersion = inputVersion;
            this.requestedVersion = requestedVersion;
            this.onlyIfLower = onlyIfLower;
        }

        public Force force;
        public PdfVersion inputVersion;
        public PdfVersion requestedVersion;
        public OnlyIfLower onlyIfLower;

        @Override
        public String toString() {
            List<String> argsList = new ArrayList<>();
            argsList.add("update-version");
            argsList.add(inputVersion.resourceName());
            if (force.toBoolean()) {
                argsList.add("--force");
            } else {
                argsList.add("--out");
                final String outFileName = "out.pdf";
                assert !outFileName.equals(inputVersion.resourceName());
                argsList.add(outFileName);
            }
            if (requestedVersion != null) {
                argsList.add("--version");
                argsList.add(requestedVersion.toString());
            }
            if (onlyIfLower.toBoolean()) {
                argsList.add("--only-if-lower");
            }
            return String.join(" ", argsList);
        }
    }

    public static List<UpdateVersionInput> updateVersionInputs() {
        List<UpdateVersionInput> result = new ArrayList<>();
        for (Force force : Force.values()) {
            for (PdfVersion inputVersion : PdfVersion.values()) {
                for (OnlyIfLower onlyIfLower : OnlyIfLower.values()) {
                    for (PdfVersion requestedVersion : PdfVersion.values()) {
                        result.add(new UpdateVersionInput(force, inputVersion, requestedVersion, onlyIfLower));
                    }
                }
            }
        }
        return result;
    }

    @DataProvider
    public static Object[][] dataProviderUpdateVersion() {
        List<Object[]> result = new ArrayList<>();
        for (final UpdateVersionInput updateVersionInput : updateVersionInputs()) {
            final Force force = updateVersionInput.force;
            assert force != null;
            final PdfVersion inputVersion = updateVersionInput.inputVersion;
            assert inputVersion != null;
            final PdfVersion requestedVersion = updateVersionInput.requestedVersion;
            final OnlyIfLower onlyIfLower = updateVersionInput.onlyIfLower;
            assert onlyIfLower != null;
            PdfVersion expectedVersion = requestedVersion;
            if (expectedVersion == null) {
                expectedVersion = PdfVersion.DEFAULT;
            }
            if (!force.toBoolean() && onlyIfLower.toBoolean() && inputVersion.compareTo(expectedVersion) >= 0) {
                // Discard combinations that do not create an output file
                continue;
            }
            if (onlyIfLower.toBoolean() && inputVersion.compareTo(expectedVersion) > 0) {
                expectedVersion = inputVersion;
            }
            result.add(new Object[]{updateVersionInput, expectedVersion});
        }
        return result.toArray(new Object[][]{});
    }

    private File outFile;

    @Test
    @UseDataProvider
    public void testUpdateVersion(final UpdateVersionInput updateVersionInput,
            final PdfVersion expectedVersion) throws IOException {
        final Force force = updateVersionInput.force;
        final PdfVersion inputVersion = updateVersionInput.inputVersion;
        final PdfVersion requestedVersion = updateVersionInput.requestedVersion;
        final OnlyIfLower onlyIfLower = updateVersionInput.onlyIfLower;
        final File document = inputVersion.getFile(folder);
        outFile = document;
        List<String> argsList = new ArrayList<>();
        argsList.add("update-version");
        argsList.add(document.getAbsolutePath());
        if (force.toBoolean()) {
            argsList.add("--force");
        } else {
            argsList.add("--out");
            final String outFileName = "out.pdf";
            assert !outFileName.equals(inputVersion.resourceName());
            outFile = folder.newFile(outFileName);
            { // success
                final boolean success = outFile.delete();
                assert success;
            }
            assert !outFile.exists();
            argsList.add(outFile.getAbsolutePath());
        }
        if (requestedVersion != null) {
            argsList.add("--version");
            argsList.add(requestedVersion.toString());
        }
        if (onlyIfLower.toBoolean()) {
            argsList.add("--only-if-lower");
        }
        exit.expectSystemExitWithStatus(0);
        exit.checkAssertionAfterwards(new Assertion() {
            @Override
            public void checkAssertion() throws OperationException, IOException {
                Inspect inspect = OperationInspect.getInstance().execute(outFile);
                Assert.assertEquals(expectedVersion.toString(), inspect.version);
            }
        });
        Main.main(argsList.toArray(new String[]{}));
        assert false;
    }

    @Ignore
    @Test
    public void testUpdateProperties() {
        Assert.fail();
    }

    @Ignore
    @Test
    public void testAttach() {
        Assert.fail();
    }

    @Ignore
    @Test
    public void testSign() {
        Assert.fail();
    }

}