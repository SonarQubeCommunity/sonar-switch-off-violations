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

import com.google.common.collect.Sets;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.WildcardPattern;

import java.util.Set;

public class Pattern {

  private WildcardPattern resourcePattern;
  private WildcardPattern rulePattern;
  private Set<Integer> lines = Sets.newLinkedHashSet();
  private Set<LineRange> lineRanges = Sets.newLinkedHashSet();
  private String beginBlockRegexp;
  private String endBlockRegexp;
  private String allFileRegexp;
  private boolean checkLines = true;

  public Pattern() {
  }

  public Pattern(String resourcePattern, String rulePattern) {
    this.resourcePattern = WildcardPattern.create(resourcePattern);
    this.rulePattern = WildcardPattern.create(rulePattern);
  }

  public Pattern(String resourcePattern, String rulePattern, Set<LineRange> lineRanges) {
    this(resourcePattern, rulePattern);
    this.lineRanges = lineRanges;
  }

  public WildcardPattern getResourcePattern() {
    return resourcePattern;
  }

  public WildcardPattern getRulePattern() {
    return rulePattern;
  }

  public String getBeginBlockRegexp() {
    return beginBlockRegexp;
  }

  public String getEndBlockRegexp() {
    return endBlockRegexp;
  }

  public String getAllFileRegexp() {
    return allFileRegexp;
  }

  Pattern addLineRange(int fromLineId, int toLineId) {
    lineRanges.add(new LineRange(fromLineId, toLineId));
    return this;
  }

  Pattern addLine(int lineId) {
    lines.add(lineId);
    return this;
  }

  boolean isCheckLines() {
    return checkLines;
  }

  Pattern setCheckLines(boolean b) {
    this.checkLines = b;
    return this;
  }

  Pattern setBeginBlockRegexp(String beginBlockRegexp) {
    this.beginBlockRegexp = beginBlockRegexp;
    return this;
  }

  Pattern setEndBlockRegexp(String endBlockRegexp) {
    this.endBlockRegexp = endBlockRegexp;
    return this;
  }

  Pattern setAllFileRegexp(String allFileRegexp) {
    this.allFileRegexp = allFileRegexp;
    return this;
  }

  Set<Integer> getAllLines() {
    Set<Integer> allLines = Sets.newLinkedHashSet(lines);
    for (LineRange lineRange : lineRanges) {
      allLines.addAll(lineRange.toLines());
    }
    return allLines;
  }

  public boolean match(Violation violation) {
    boolean match = matchResource(violation.getResource()) && matchRule(violation.getRule());
    if (checkLines && violation.getLineId() != null) {
      match = match && matchLine(violation.getLineId());
    }
    return match;
  }

  boolean matchLine(int lineId) {
    if (lines.contains(lineId)) {
      return true;
    }

    for (LineRange range : lineRanges) {
      if (range.in(lineId)) {
        return true;
      }
    }

    return false;
  }

  boolean matchRule(Rule rule) {
    if (rule == null) {
      return false;
    }

    String key = new StringBuilder().append(rule.getRepositoryKey()).append(':').append(rule.getKey()).toString();
    return rulePattern.match(key);
  }

  boolean matchResource(Resource<?> resource) {
    return resource != null && resource.getKey() != null && resourcePattern.match(resource.getKey());
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
