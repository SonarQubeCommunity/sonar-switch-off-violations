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

import org.junit.Test;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PatternTest {

  @Test
  public void shouldMatchLines() {
    Pattern pattern = new Pattern("*", "*");
    pattern.addLine(12).addLine(15).addLineRange(20, 25);

    assertThat(pattern.matchLine(3)).isFalse();
    assertThat(pattern.matchLine(12)).isTrue();
    assertThat(pattern.matchLine(14)).isFalse();
    assertThat(pattern.matchLine(21)).isTrue();
    assertThat(pattern.matchLine(6599)).isFalse();
  }

  @Test
  public void shouldMatchJavaFile() {
    JavaFile javaFile = new JavaFile("org.foo.Bar");
    assertThat(new Pattern("org.foo.Bar", "*").matchResource(javaFile)).isTrue();
    assertThat(new Pattern("org.foo.*", "*").matchResource(javaFile)).isTrue();
    assertThat(new Pattern("*Bar", "*").matchResource(javaFile)).isTrue();
    assertThat(new Pattern("*", "*").matchResource(javaFile)).isTrue();
    assertThat(new Pattern("org.*.?ar", "*").matchResource(javaFile)).isTrue();

    assertThat(new Pattern("org.other.Hello", "*").matchResource(javaFile)).isFalse();
    assertThat(new Pattern("org.foo.Hello", "*").matchResource(javaFile)).isFalse();
    assertThat(new Pattern("org.*.??ar", "*").matchResource(javaFile)).isFalse();
    assertThat(new Pattern("org.*.??ar", "*").matchResource(null)).isFalse();
    assertThat(new Pattern("org.*.??ar", "*").matchResource(mock(Resource.class))).isFalse();
  }

  @Test
  public void shouldMatchRule() {
    Rule rule = Rule.create("checkstyle", "IllegalRegexp", "");
    assertThat(new Pattern("*", "*").matchRule(rule)).isTrue();
    assertThat(new Pattern("*", "checkstyle:*").matchRule(rule)).isTrue();
    assertThat(new Pattern("*", "checkstyle:IllegalRegexp").matchRule(rule)).isTrue();
    assertThat(new Pattern("*", "checkstyle:Illegal*").matchRule(rule)).isTrue();
    assertThat(new Pattern("*", "*:*Illegal*").matchRule(rule)).isTrue();

    assertThat(new Pattern("*", "pmd:IllegalRegexp").matchRule(rule)).isFalse();
    assertThat(new Pattern("*", "pmd:*").matchRule(rule)).isFalse();
    assertThat(new Pattern("*", "*:Foo*IllegalRegexp").matchRule(rule)).isFalse();
  }

  @Test
  public void shouldMatchViolation() {
    Rule rule = Rule.create("checkstyle", "IllegalRegexp", "");
    JavaFile javaFile = new JavaFile("org.foo.Bar");

    Pattern pattern = new Pattern("*", "*");
    pattern.addLine(12);

    assertThat(pattern.match(Violation.create(rule, javaFile))).isTrue();
    assertThat(pattern.match(Violation.create(rule, javaFile).setLineId(12))).isTrue();
    assertThat(pattern.match(Violation.create((Rule) null, javaFile).setLineId(5))).isFalse();
    assertThat(pattern.match(Violation.create(rule, null))).isFalse();
    assertThat(pattern.match(Violation.create((Rule) null, null))).isFalse();
  }

  @Test
  public void shouldNotMatchNullRule() {
    assertThat(new Pattern("*", "*").matchRule(null)).isFalse();
  }

  @Test
  public void shouldPrintPatternToString() {
    Pattern pattern = new Pattern("*", "checkstyle:*");

    assertThat(pattern.toString()).isEqualTo("Pattern[resourcePattern=*,rulePattern=checkstyle:*,lines=[],lineRanges=[],beginBlockRegexp=<null>,endBlockRegexp=<null>,allFileRegexp=<null>,checkLines=true]");
  }
}
