/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/** Bug checker to detect usage of {@code Set<T[]>} or {@code Map<T[], E>}. */
@BugPattern(
    name = "ArrayAsKeyOfSetOrMap",
    summary =
        "Arrays do not override equals() or hashCode, so comparisons will be done on"
            + " reference equality only. If neither deduplication nor lookup are needed, "
            + "consider using a List instead. Otherwise, use IdentityHashMap/Set, "
            + "a Map from a library that handles object arrays, or an Iterable/List of pairs.",
    severity = WARNING)

/**
 * Warns that users should not have an array as a key to a Set or Map
 *
 * @author siyuanl@google.com (Siyuan Liu)
 * @author eleanorh@google.com (Eleanor Harris)
 */
public class ArrayAsKeyOfSetOrMap extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> CONSTRUCTS_HASHSET_OR_HASHMAP =
      anyOf(
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.Sets")
              .named("newHashSet"),
          MethodMatchers.staticMethod()
              .onClass("com.google.common.collect.Maps")
              .named("newHashMap"),
          Matchers.constructor().forClass("java.util.HashMap"),
          Matchers.constructor().forClass("java.util.HashSet"));

  private Description matchArrays(ExpressionTree tree, VisitorState state) {
    if (!CONSTRUCTS_HASHSET_OR_HASHMAP.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    List<Type> argumentTypes = ASTHelpers.getResultType(tree).getTypeArguments();
    if (argumentTypes.isEmpty()) {
      return Description.NO_MATCH;
    }
    Type firstArgumentType = argumentTypes.get(0);
    return firstArgumentType instanceof Type.ArrayType ? describeMatch(tree) : Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchArrays(tree, state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchArrays(tree, state);
  }
}
