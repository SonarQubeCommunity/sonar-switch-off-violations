/*
 * Sonar Switch Off Violations Plugin
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.switchoffviolations.scanner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.switchoffviolations.pattern.Pattern;
import org.sonar.plugins.switchoffviolations.pattern.PatternsInitializer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static com.google.common.base.Charsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SourceScannerTest {

  private SourceScanner scanner;

  @Mock
  private RegexpScanner regexpScanner;
  @Mock
  private PatternsInitializer patternsInitializer;
  @Mock
  private Project project;
  @Mock
  private ProjectFileSystem fileSystem;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    when(fileSystem.getSourceCharset()).thenReturn(UTF_8);

    scanner = new SourceScanner(regexpScanner, patternsInitializer, fileSystem);
  }

  @Test
  public void testToString() throws Exception {
    assertThat(scanner.toString()).isEqualTo("Switch Off Plugin - Source Scanner");
  }

  @Test
  public void shouldExecute() throws IOException {
    when(patternsInitializer.getAllFilePatterns()).thenReturn(Arrays.asList(new Pattern(), new Pattern()));
    assertThat(scanner.shouldExecuteOnProject(null)).isTrue();

    when(patternsInitializer.getAllFilePatterns()).thenReturn(Collections.<Pattern>emptyList());
    when(patternsInitializer.getBlockPatterns()).thenReturn(Arrays.asList(new Pattern(), new Pattern()));
    assertThat(scanner.shouldExecuteOnProject(null)).isTrue();

    when(patternsInitializer.getAllFilePatterns()).thenReturn(Collections.<Pattern>emptyList());
    when(patternsInitializer.getBlockPatterns()).thenReturn(Collections.<Pattern>emptyList());
    assertThat(scanner.shouldExecuteOnProject(null)).isFalse();
  }

  @Test
  public void shouldAnalyseJavaProject() throws IOException {
    File sourceFile = new File("Foo.java");
    File testFile = new File("FooTest.java");

    when(project.getLanguageKey()).thenReturn("java");
    when(fileSystem.mainFiles("java")).thenReturn(Arrays.asList(inputFile(sourceFile)));
    when(fileSystem.testFiles("java")).thenReturn(Arrays.asList(inputFile(testFile)));

    scanner.analyse(project, null);

    verify(regexpScanner).scan(new JavaFile("[default].Foo"), sourceFile, UTF_8);
    verify(regexpScanner).scan(new JavaFile("[default].FooTest", true), testFile, UTF_8);
  }

  @Test
  public void shouldAnalyseOtherProject() throws IOException {
    File sourceFile = new File("Foo.php");
    File testFile = new File("FooTest.php");

    when(project.getLanguageKey()).thenReturn("php");
    when(fileSystem.mainFiles("php")).thenReturn(Arrays.asList(inputFile(sourceFile)));
    when(fileSystem.testFiles("php")).thenReturn(Arrays.asList(inputFile(testFile)));

    scanner.analyse(project, null);

    verify(regexpScanner).scan(new org.sonar.api.resources.File("Foo.php"), sourceFile, UTF_8);
    verify(regexpScanner).scan(new org.sonar.api.resources.File("FooTest.php"), testFile, UTF_8);
  }

  @Test
  public void shouldAnalyseJavaProjectWithNonJavaFile() throws IOException {
    File sourceFile = new File("Foo.java");
    File otherFile = new File("other.js");

    when(project.getLanguageKey()).thenReturn("java");
    when(fileSystem.mainFiles("java")).thenReturn(Arrays.asList(inputFile(sourceFile), inputFile(otherFile)));

    scanner.analyse(project, null);

    verify(regexpScanner, never()).scan(new org.sonar.api.resources.File("other.js"), sourceFile, UTF_8);
  }

  @Test
  public void shouldAnalyseJavaProjectWithInvalidFile() throws IOException {
    InputFile inputFile = invalidInputFile();

    when(project.getLanguageKey()).thenReturn("java");
    when(fileSystem.mainFiles("java")).thenReturn(Arrays.asList(inputFile));

    scanner.analyse(project, null);

    verifyZeroInteractions(regexpScanner);
  }

  @Test
  public void shouldReportFailure() throws IOException {
    File sourceFile = new File("Foo.php");

    when(project.getLanguageKey()).thenReturn("php");
    when(fileSystem.mainFiles("php")).thenReturn(Arrays.asList(inputFile(sourceFile)));
    doThrow(new IOException("BUG")).when(regexpScanner).scan(new org.sonar.api.resources.File("Foo.php"), sourceFile, UTF_8);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Unable to read the source file");

    scanner.analyse(project, null);
  }

  private static InputFile inputFile(File file) {
    return InputFileUtils.create(null, file.getName());
  }

  private static InputFile invalidInputFile() {
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.getFile()).thenReturn(new File("invalid.java"));
    return inputFile;
  }
}
