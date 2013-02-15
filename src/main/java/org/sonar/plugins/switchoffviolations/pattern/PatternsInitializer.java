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

package org.sonar.plugins.switchoffviolations.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.switchoffviolations.Constants;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;

public class PatternsInitializer implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(PatternsInitializer.class);

  private final Settings settings;
  private final ProjectFileSystem projectFileSystem;

  private List<Pattern> multicriteriaPatterns;
  private List<Pattern> blockPatterns;
  private List<Pattern> allFilePatterns;
  private Map<Resource<?>, Pattern> extraPatternByResource = Maps.newHashMap();

  public PatternsInitializer(Settings settings, ProjectFileSystem projectFileSystem) {
    this.settings = settings;
    this.projectFileSystem = projectFileSystem;
    initPatterns();
  }

  public List<Pattern> getMulticriteriaPatterns() {
    return multicriteriaPatterns;
  }

  public List<Pattern> getBlockPatterns() {
    return blockPatterns;
  }

  public List<Pattern> getAllFilePatterns() {
    return allFilePatterns;
  }

  public Pattern getExtraPattern(Resource<?> resource) {
    return extraPatternByResource.get(resource);
  }

  @VisibleForTesting
  protected final void initPatterns() {
    multicriteriaPatterns = Lists.newArrayList();
    blockPatterns = Lists.newArrayList();
    allFilePatterns = Lists.newArrayList();

    loadPatternsFromNewProperties();
    loadPatternsFromDeprecatedProperties();
  }

  private void loadPatternsFromNewProperties() {
    // Patterns Multicriteria
    String patternConf = StringUtils.defaultIfBlank(settings.getString(Constants.PATTERNS_MULTICRITERIA_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = Constants.PATTERNS_MULTICRITERIA_KEY + "." + id + ".";
      String resourceKeyPattern = settings.getString(propPrefix + Constants.RESOURCE_KEY);
      String ruleKeyPattern = settings.getString(propPrefix + Constants.RULE_KEY);
      Pattern pattern = new Pattern(firstNonNull(resourceKeyPattern, "*"), firstNonNull(ruleKeyPattern, "*"));
      String lineRange = settings.getString(propPrefix + Constants.LINE_RANGE_KEY);
      PatternDecoder.decodeRangeOfLines(pattern, firstNonNull(lineRange, "*"));
      multicriteriaPatterns.add(pattern);
    }

    // Patterns Block
    patternConf = StringUtils.defaultIfBlank(settings.getString(Constants.PATTERNS_BLOCK_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = Constants.PATTERNS_BLOCK_KEY + "." + id + ".";
      String beginBlockRegexp = settings.getString(propPrefix + Constants.BEGIN_BLOCK_REGEXP);
      String endBlockRegexp = settings.getString(propPrefix + Constants.END_BLOCK_REGEXP);
      Pattern pattern = new Pattern().setBeginBlockRegexp(nullToEmpty(beginBlockRegexp)).setEndBlockRegexp(nullToEmpty(endBlockRegexp));
      blockPatterns.add(pattern);
    }

    // Patterns All File
    patternConf = StringUtils.defaultIfBlank(settings.getString(Constants.PATTERNS_ALLFILE_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = Constants.PATTERNS_ALLFILE_KEY + "." + id + ".";
      String allFileRegexp = settings.getString(propPrefix + Constants.FILE_REGEXP);
      Pattern pattern = new Pattern().setAllFileRegexp(nullToEmpty(allFileRegexp));
      allFilePatterns.add(pattern);
    }
  }

  private void loadPatternsFromDeprecatedProperties() {
    String patternConf = settings.getString(Constants.PATTERNS_PARAMETER_KEY);
    String fileLocation = settings.getString(Constants.LOCATION_PARAMETER_KEY);
    List<Pattern> list = Lists.newArrayList();
    if (StringUtils.isNotBlank(patternConf)) {
      list = new PatternDecoder().decode(patternConf);
    } else if (StringUtils.isNotBlank(fileLocation)) {
      File file = locateFile(fileLocation);
      LOG.info("Switch Off Violations plugin configured with: " + file.getAbsolutePath());
      list = new PatternDecoder().decode(file);
    }

    for (Pattern pattern : list) {
      if (pattern.getResourcePattern() != null) {
        multicriteriaPatterns.add(pattern);
      } else if (pattern.getBeginBlockRegexp() != null) {
        blockPatterns.add(pattern);
      } else {
        allFilePatterns.add(pattern);
      }
    }
  }

  private File locateFile(String location) {
    File file = new File(projectFileSystem.getBasedir(), location);
    if (!file.isFile()) {
      throw new SonarException("File not found. Please check the parameter " + Constants.LOCATION_PARAMETER_KEY + ": " + location);
    }
    return file;
  }

  public void addPatternToExcludeResource(Resource<?> resource) {
    extraPatternByResource.put(resource, new Pattern(resource.getKey(), "*").setCheckLines(false));
  }

  public void addPatternToExcludeLines(Resource<?> resource, Set<LineRange> lineRanges) {
    extraPatternByResource.put(resource, new Pattern(resource.getKey(), "*", lineRanges));
  }

}
