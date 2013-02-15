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

import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.switchoffviolations.pattern.PatternsInitializer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

@Phase(name = Phase.Name.PRE)
public final class SourceScanner implements Sensor {

  private final RegexpScanner regexpScanner;
  private final PatternsInitializer patternsInitializer;
  private final ProjectFileSystem fileSystem;

  public SourceScanner(RegexpScanner regexpScanner, PatternsInitializer patternsInitializer, ProjectFileSystem fileSystem) {
    this.regexpScanner = regexpScanner;
    this.patternsInitializer = patternsInitializer;
    this.fileSystem = fileSystem;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return patternsInitializer.getAllFilePatterns().size() > 0 || patternsInitializer.getBlockPatterns().size() > 0;
  }

  /**
   * {@inheritDoc}
   */
  public void analyse(Project project, SensorContext context) {
    parseDirs(project, false);
    parseDirs(project, true);
  }

  protected void parseDirs(Project project, boolean isTest) {
    Charset sourcesEncoding = fileSystem.getSourceCharset();

    List<InputFile> files;
    if (isTest) {
      files = fileSystem.testFiles(project.getLanguageKey());
    } else {
      files = fileSystem.mainFiles(project.getLanguageKey());
    }

    for (InputFile inputFile : files) {
      Resource<?> resource = defineResource(inputFile, project, isTest);
      if (resource != null) {
        File file = inputFile.getFile();
        try {
          regexpScanner.scan(resource, file, sourcesEncoding);
        } catch (Exception e) {
          throw new SonarException("Unable to read the source file : '" + file.getAbsolutePath() + "' with the charset : '"
            + sourcesEncoding.name() + "'.", e);
        }
      }
    }
  }

  /*
   * This method is necessary because Java resources are not treated as every other resource...
   */
  private Resource<?> defineResource(InputFile inputFile, Project project, boolean isTest) {
    if (Java.KEY.equals(project.getLanguageKey()) && Java.isJavaFile(inputFile.getFile())) {
      return JavaFile.fromRelativePath(inputFile.getRelativePath(), isTest);
    }
    return new org.sonar.api.resources.File(inputFile.getRelativePath());
  }

  @Override
  public String toString() {
    return "Switch Off Plugin - Source Scanner";
  }

}
