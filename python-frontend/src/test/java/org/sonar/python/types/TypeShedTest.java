/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python.types;

import com.google.protobuf.TextFormat;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.plugins.python.api.ProjectPythonVersion;
import org.sonar.plugins.python.api.PythonVersionUtils;
import org.sonar.plugins.python.api.symbols.AmbiguousSymbol;
import org.sonar.plugins.python.api.symbols.ClassSymbol;
import org.sonar.plugins.python.api.symbols.FunctionSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.symbols.Symbol.Kind;
import org.sonar.python.semantic.AmbiguousSymbolImpl;
import org.sonar.python.semantic.ClassSymbolImpl;
import org.sonar.python.semantic.FunctionSymbolImpl;
import org.sonar.python.semantic.SymbolImpl;
import org.sonar.python.types.protobuf.SymbolsProtos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.python.types.TypeShed.symbolsForModule;

public class TypeShedTest {

  @org.junit.Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setPythonVersions() {
    ProjectPythonVersion.setCurrentVersions(PythonVersionUtils.allVersions());
    TypeShed.resetBuiltinSymbols();
  }

  private void setPythonVersions(Set<PythonVersionUtils.Version> pythonVersions) {
    ProjectPythonVersion.setCurrentVersions(pythonVersions);
    TypeShed.resetBuiltinSymbols();
  }

  @Test
  public void classes() {
    ClassSymbol intClass = TypeShed.typeShedClass("int");
    assertThat(intClass.superClasses()).extracting(Symbol::name).containsExactly("object");
    assertThat(intClass.hasUnresolvedTypeHierarchy()).isFalse();
    assertThat(intClass.usages()).isEmpty();
    assertThat(intClass.declaredMembers()).allMatch(member -> member.usages().isEmpty());
    assertThat(TypeShed.typeShedClass("bool").superClasses()).extracting(Symbol::fullyQualifiedName).containsExactly("int");
  }

  @Test
  public void str() {
    ClassSymbol strClass = TypeShed.typeShedClass("str");
    assertThat(strClass.hasUnresolvedTypeHierarchy()).isFalse();
    assertThat(strClass.superClasses()).extracting(Symbol::kind, Symbol::name).containsExactlyInAnyOrder(tuple(Kind.AMBIGUOUS, "Sequence"));
    // python 3.9 support
    assertThat(strClass.resolveMember("removeprefix")).isNotEmpty();
    assertThat(strClass.resolveMember("removesuffix")).isNotEmpty();

    setPythonVersions(PythonVersionUtils.fromString("3.8"));
    strClass = TypeShed.typeShedClass("str");
    assertThat(strClass.superClasses()).extracting(Symbol::kind, Symbol::name).containsExactlyInAnyOrder(tuple(Kind.CLASS, "Sequence"));
    assertThat(strClass.resolveMember("removeprefix")).isEmpty();
    assertThat(strClass.resolveMember("removesuffix")).isEmpty();

    setPythonVersions(PythonVersionUtils.fromString("2.7"));
    strClass = TypeShed.typeShedClass("str");
    assertThat(strClass.superClasses()).extracting(Symbol::kind, Symbol::name)
      .containsExactlyInAnyOrder(tuple(Kind.CLASS, "Sequence"), tuple(Kind.CLASS, "basestring"));

    setPythonVersions(PythonVersionUtils.allVersions());
  }

  @Test(expected = IllegalArgumentException.class)
  public void not_a_class() {
    TypeShed.typeShedClass("repr");
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknown_name() {
    TypeShed.typeShedClass("xxx");
  }

  @Test
  public void none_type() {
    ClassSymbol noneType = TypeShed.typeShedClass("NoneType");
    assertThat(noneType.superClasses()).isEmpty();
  }

  @Test
  public void typing_module() {
    Map<String, Symbol> symbols = symbolsForModule("typing");
    assertThat(symbols.values()).allMatch(symbol -> symbol.usages().isEmpty());
    // python3 specific
    assertThat(symbols.get("Awaitable").kind()).isEqualTo(Kind.CLASS);
    // overlap btw python2 and python3
    assertThat(symbols.get("Sequence").kind()).isEqualTo(Kind.AMBIGUOUS);
  }

  @Test
  public void stdlib_symbols() {
    Map<String, Symbol> mathSymbols = symbolsForModule("math");
    Symbol acosSymbol = mathSymbols.get("acos");
    assertThat(acosSymbol.kind()).isEqualTo(Kind.FUNCTION);
    assertThat(((FunctionSymbolImpl) acosSymbol).declaredReturnType().canOnlyBe("float")).isTrue();
    assertThat(TypeShed.symbolWithFQN("math", "math.acos")).isSameAs(acosSymbol);
    assertThat(mathSymbols.values()).allMatch(symbol -> symbol.usages().isEmpty());

    Map<String, Symbol> threadingSymbols = symbolsForModule("threading");
    assertThat(threadingSymbols.get("Thread").kind()).isEqualTo(Kind.CLASS);
    assertThat(threadingSymbols.values()).allMatch(symbol -> symbol.usages().isEmpty());

    Map<String, Symbol> imaplibSymbols = symbolsForModule("imaplib");
    assertThat(imaplibSymbols).isNotEmpty();
    assertThat(imaplibSymbols.values()).allMatch(symbol -> symbol.usages().isEmpty());
  }

  @Test
  public void third_party_symbols() {
    Map<String, Symbol> emojiSymbols = symbolsForModule("emoji");
    Symbol emojizeSymbol = emojiSymbols.get("emojize");
    assertThat(emojizeSymbol.kind()).isEqualTo(Kind.FUNCTION);
    assertThat(((FunctionSymbolImpl) emojizeSymbol).declaredReturnType().canOnlyBe("str")).isTrue();
    assertThat(TypeShed.symbolWithFQN("emoji", "emoji.emojize")).isSameAs(emojizeSymbol);
  }

  @Test
  public void should_resolve_packages() {
    assertThat(symbolsForModule("urllib")).isNotEmpty();
    assertThat(symbolsForModule("ctypes")).isNotEmpty();
    assertThat(symbolsForModule("email")).isNotEmpty();
    assertThat(symbolsForModule("json")).isNotEmpty();
    assertThat(symbolsForModule("docutils")).isNotEmpty();
    assertThat(symbolsForModule("ctypes.util")).isNotEmpty();
    assertThat(symbolsForModule("lib2to3.pgen2.grammar")).isNotEmpty();
    // resolved but still empty
    assertThat(symbolsForModule("cryptography")).isEmpty();
    assertThat(symbolsForModule("kazoo")).isEmpty();
  }

  @Test
  public void package_symbols() {
    Map<String, Symbol> cursesSymbols = symbolsForModule("curses");
    Symbol wrapperSymbol = cursesSymbols.get("wrapper");
    assertThat(wrapperSymbol.kind()).isEqualTo(Kind.FUNCTION);
    assertThat(((FunctionSymbolImpl) wrapperSymbol).declaredReturnType()).isEqualTo(AnyType.ANY);
    assertThat(TypeShed.symbolWithFQN("curses", "curses.wrapper")).isSameAs(wrapperSymbol);
  }

  @Test
  public void package_submodules_symbols() {
    Map<String, Symbol> asciiSymbols = symbolsForModule("curses.ascii");
    Symbol isalnumSymbol = asciiSymbols.get("isalnum");
    assertThat(isalnumSymbol.kind()).isEqualTo(Kind.FUNCTION);
    assertThat(((FunctionSymbolImpl) isalnumSymbol).declaredReturnType().canOnlyBe("bool")).isTrue();
    assertThat(TypeShed.symbolWithFQN("curses.ascii", "curses.ascii.isalnum")).isSameAs(isalnumSymbol);
  }

  @Test
  public void package_inner_submodules_symbols() {
    Map<String, Symbol> driverSymbols = symbolsForModule("lib2to3.pgen2.driver");
    Symbol loadGrammarSymbol = driverSymbols.get("load_grammar");
    // There is a small difference between Python 2 and Python 3 symbols: Python 2 uses Text instead of str
    assertThat(loadGrammarSymbol.kind()).isEqualTo(Kind.AMBIGUOUS);
    assertThat(TypeShed.symbolWithFQN("lib2to3.pgen2.driver", "lib2to3.pgen2.driver.load_grammar")).isSameAs(loadGrammarSymbol);
  }

  @Test
  public void package_relative_import() {
    Map<String, Symbol> osSymbols = symbolsForModule("os");
    // The "import sys" is not part of the exported API (private import) in Typeshed
    // See: https://github.com/python/typeshed/blob/master/CONTRIBUTING.md#conventions
    assertThat(osSymbols).doesNotContainKey("sys");

    Map<String, Symbol> sqlite3Symbols = symbolsForModule("sqlite3");
    Symbol completeStatementFunction = sqlite3Symbols.get("complete_statement");
    assertThat(completeStatementFunction.kind()).isEqualTo(Kind.FUNCTION);
    assertThat(completeStatementFunction.fullyQualifiedName()).isEqualTo("sqlite3.dbapi2.complete_statement");
    Set<String> sqlite3Dbapi2Symbols = symbolsForModule("sqlite3.dbapi2").keySet();
    // Python names with a leading underscore are not imported when using wildcard imports
    sqlite3Dbapi2Symbols.removeIf(s -> s.startsWith("_"));
    assertThat(sqlite3Symbols.keySet()).containsAll(sqlite3Dbapi2Symbols);

    Map<String, Symbol> requestsSymbols = symbolsForModule("requests");
    Symbol requestSymbol = requestsSymbols.get("request");
    assertThat(requestSymbol.kind()).isEqualTo(Kind.FUNCTION);
    assertThat(requestSymbol.fullyQualifiedName()).isEqualTo("requests.api.request");
  }

  @Test
  public void package_member_fqn_points_to_original_fqn() {
    Map<String, Symbol> symbols = symbolsForModule("flask");
    Symbol targetSymbol = symbols.get("Response");
    assertThat(targetSymbol.fullyQualifiedName()).isEqualTo("flask.wrappers.Response");
    assertThat(TypeShed.symbolWithFQN("flask", "flask.Response")).isSameAs(targetSymbol);
  }


  @Test
  public void package_member_ambigous_symbol_common_fqn() {
    Map<String, Symbol> symbols = symbolsForModule("io");
    Symbol targetSymbol = symbols.get("FileIO");
    assertThat(targetSymbol.fullyQualifiedName()).isEqualTo("io.FileIO");
    assertThat(TypeShed.symbolWithFQN("io", "io.FileIO")).isSameAs(targetSymbol);
  }

  @Test
  public void two_exported_symbols_with_same_local_names() {
    Map<String, Symbol> osSymbols = symbolsForModule("os");
    Map<String, Symbol> posixSymbols = symbolsForModule("posix");
    Symbol setupSymbolFromPosix = posixSymbols.get("stat_result");
    Symbol setupSymbolFromOs = osSymbols.get("stat_result");
    assertThat(setupSymbolFromPosix.kind()).isEqualTo(Kind.AMBIGUOUS);
    assertThat(setupSymbolFromPosix.fullyQualifiedName()).isEqualTo("posix.stat_result");
    assertThat(setupSymbolFromOs.kind()).isEqualTo(Kind.AMBIGUOUS);
    assertThat(setupSymbolFromOs.fullyQualifiedName()).isEqualTo("os.stat_result");
  }

  @Test
  public void package_django() {
    Map<String, Symbol> djangoSymbols = symbolsForModule("django.http");
    Symbol responseSymbol = djangoSymbols.get("HttpResponse");
    assertThat(responseSymbol.kind()).isEqualTo(Kind.CLASS);
    assertThat(responseSymbol.fullyQualifiedName()).isEqualTo("django.http.response.HttpResponse");
  }

  @Test
  public void return_type_hints() {
    Map<String, Symbol> symbols = symbolsForModule("typing");
    assertThat(((FunctionSymbolImpl) symbols.get("get_args")).annotatedReturnTypeName()).isEqualTo("tuple");
    symbols = symbolsForModule("flask_mail");
    ClassSymbol mail = (ClassSymbol) symbols.get("Mail");
    assertThat(((FunctionSymbol) mail.declaredMembers().stream().iterator().next()).annotatedReturnTypeName()).isNull();
  }

  @Test
  public void package_django_class_property_type() {
    Map<String, Symbol> djangoSymbols = symbolsForModule("django.http.request");
    Symbol requestSymbol = djangoSymbols.get("HttpRequest");
    assertThat(requestSymbol.kind()).isEqualTo(Kind.CLASS);
    assertThat(((ClassSymbol) requestSymbol).declaredMembers().iterator().next().annotatedTypeName()).isEqualTo("django.http.request.QueryDict");
  }

  @Test
  public void package_lxml_reexported_symbol_fqn() {
    Map<String, Symbol> lxmlEtreeSymbols = symbolsForModule("lxml.etree");
    Symbol elementTreeSymbol = lxmlEtreeSymbols.get("ElementTree");
    assertThat(elementTreeSymbol.kind()).isEqualTo(Kind.CLASS);
    // FIXME: Original FQN is "xml.etree.ElementTree.ElementTree" and we should be able to retrieve it somehow
    assertThat(elementTreeSymbol.fullyQualifiedName()).isEqualTo("lxml.etree.ElementTree");
  }

  @Test
  public void package_sqlite3_connect_type_in_ambiguous_symbol() {
    Map<String, Symbol> djangoSymbols = symbolsForModule("sqlite3");
    Symbol requestSymbol = djangoSymbols.get("connect");
    assertThat(((FunctionSymbolImpl) ((((AmbiguousSymbolImpl) requestSymbol).alternatives()).toArray()[0])).annotatedReturnTypeName()).isEqualTo("sqlite3.dbapi2.Connection");
  }

  @Test
  public void stub_files_symbols() {
    Collection<Symbol> mathSymbols = symbolsForModule("math").values();
    Collection<Symbol> djangoHttpSymbols = symbolsForModule("django.http").values();

    Collection<Symbol> symbols = TypeShed.stubFilesSymbols();
    assertThat(symbols)
      .containsAll(mathSymbols)
      .containsAll(djangoHttpSymbols);
  }

  @Test
  public void deserialize_annoy_protobuf() {
    Map<String, Symbol> deserializedAnnoySymbols = symbolsForModule("annoy").values().stream()
      .collect(Collectors.toMap(Symbol::fullyQualifiedName, s -> s));
    assertThat(deserializedAnnoySymbols.values()).extracting(Symbol::kind, Symbol::fullyQualifiedName)
      .containsExactlyInAnyOrder(tuple(Kind.CLASS, "annoy._Vector"), tuple(Kind.CLASS, "annoy.AnnoyIndex"));

    ClassSymbol vector = (ClassSymbol) deserializedAnnoySymbols.get("annoy._Vector");
    assertThat(vector.superClasses()).extracting(Symbol::kind, Symbol::fullyQualifiedName)
      .containsExactlyInAnyOrder(tuple(Kind.CLASS, "typing.Sized"));
    assertThat(vector.declaredMembers()).extracting(Symbol::name).containsExactlyInAnyOrder("__getitem__");
    assertThat(vector.hasDecorators()).isFalse();
    assertThat(vector.definitionLocation()).isNull();

    ClassSymbol annoyIndex = (ClassSymbol) deserializedAnnoySymbols.get("annoy.AnnoyIndex");
    assertThat(annoyIndex.superClasses()).extracting(Symbol::kind, Symbol::fullyQualifiedName)
      .containsExactlyInAnyOrder(tuple(Kind.CLASS, "object"));
    assertThat(annoyIndex.declaredMembers()).extracting(Symbol::kind, Symbol::name).containsExactlyInAnyOrder(
      tuple(Kind.FUNCTION, "__init__"),
      tuple(Kind.FUNCTION, "load"),
      tuple(Kind.FUNCTION, "save"),
      tuple(Kind.AMBIGUOUS, "get_nns_by_item"),
      tuple(Kind.AMBIGUOUS, "get_nns_by_vector"),
      tuple(Kind.FUNCTION, "get_item_vector"),
      tuple(Kind.FUNCTION, "add_item"),
      tuple(Kind.FUNCTION, "on_disk_build"),
      tuple(Kind.FUNCTION, "build"),
      tuple(Kind.FUNCTION, "unbuild"),
      tuple(Kind.FUNCTION, "unload"),
      tuple(Kind.FUNCTION, "get_distance"),
      tuple(Kind.FUNCTION, "get_n_items"),
      tuple(Kind.FUNCTION, "get_n_trees"),
      tuple(Kind.FUNCTION, "verbose"),
      tuple(Kind.FUNCTION, "set_seed"),
      tuple(Kind.OTHER, "f")
    );
    assertThat(annoyIndex.hasDecorators()).isFalse();
    assertThat(annoyIndex.definitionLocation()).isNull();
  }

  @Test
  public void deserialize_nonexistent_or_incorrect_protobuf() {
    assertThat(symbolsForModule("NOT_EXISTENT")).isEmpty();
    assertThat(TypeShed.getSymbolsFromProtobufModule(null)).isEmpty();
    InputStream targetStream = new ByteArrayInputStream("foo".getBytes());
    assertThat(TypeShed.deserializedModule("mod", targetStream)).isNull();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Error while deserializing protobuf for module mod");
  }

  @Test
  public void class_symbols_from_protobuf() throws TextFormat.ParseException {
    SymbolsProtos.ModuleSymbol moduleSymbol = moduleSymbol(
      "fully_qualified_name: \"mod\"\n" +
      "classes {\n" +
      "  name: \"Base\"\n" +
      "  fully_qualified_name: \"mod.Base\"\n" +
      "  super_classes: \"builtins.object\"\n" +
      "}\n" +
      "classes {\n" +
      "  name: \"C\"\n" +
      "  fully_qualified_name: \"mod.C\"\n" +
      "  super_classes: \"builtins.str\"\n" +
      "}\n" +
      "classes {\n" +
      "  name: \"D\"\n" +
      "  fully_qualified_name: \"mod.D\"\n" +
      "  super_classes: \"NOT_EXISTENT\"\n" +
      "}");
    Map<String, Symbol> symbols = TypeShed.getSymbolsFromProtobufModule(moduleSymbol);
    assertThat(symbols.values()).extracting(Symbol::kind, Symbol::fullyQualifiedName)
      .containsExactlyInAnyOrder(tuple(Kind.CLASS, "mod.Base"), tuple(Kind.CLASS, "mod.C"), tuple(Kind.CLASS, "mod.D"));

    ClassSymbol C = (ClassSymbol) symbols.get("C");
    assertThat(C.superClasses()).extracting(Symbol::fullyQualifiedName).containsExactly("str");
    ClassSymbol D = (ClassSymbol) symbols.get("D");
    assertThat(D.superClasses()).extracting(Symbol::kind, Symbol::fullyQualifiedName)
      .containsExactly(tuple(Kind.OTHER, "NOT_EXISTENT"));
  }

  @Test
  public void function_symbols_from_protobuf() throws TextFormat.ParseException {
    SymbolsProtos.ModuleSymbol moduleSymbol = moduleSymbol(
      "fully_qualified_name: \"mod\"\n" +
      "functions {\n" +
      "  name: \"foo\"\n" +
      "  fully_qualified_name: \"mod.foo\"\n" +
      "  parameters {\n" +
      "    name: \"p\"\n" +
      "    kind: POSITIONAL_OR_KEYWORD\n" +
      "  }\n" +
      "}\n" +
      "overloaded_functions {\n" +
      "  name: \"bar\"\n" +
      "  fullname: \"mod.bar\"\n" +
      "  definitions {\n" +
      "    name: \"bar\"\n" +
      "    fully_qualified_name: \"mod.bar\"\n" +
      "    parameters {\n" +
      "      name: \"x\"\n" +
      "      kind: POSITIONAL_OR_KEYWORD\n" +
      "    }\n" +
      "    has_decorators: true\n" +
      "    resolved_decorator_names: \"typing.overload\"\n" +
      "    is_overload: true\n" +
      "  }\n" +
      "  definitions {\n" +
      "    name: \"bar\"\n" +
      "    fully_qualified_name: \"mod.bar\"\n" +
      "    parameters {\n" +
      "      name: \"x\"\n" +
      "      kind: POSITIONAL_OR_KEYWORD\n" +
      "    }\n" +
      "    parameters {\n" +
      "      name: \"y\"\n" +
      "      kind: POSITIONAL_OR_KEYWORD\n" +
      "    }\n" +
      "    has_decorators: true\n" +
      "    resolved_decorator_names: \"typing.overload\"\n" +
      "    is_overload: true\n" +
      "  }\n" +
      "}\n");
    Map<String, Symbol> symbols = TypeShed.getSymbolsFromProtobufModule(moduleSymbol);
    assertThat(symbols.values()).extracting(Symbol::kind, Symbol::fullyQualifiedName)
      .containsExactlyInAnyOrder(tuple(Kind.FUNCTION, "mod.foo"), tuple(Kind.AMBIGUOUS, "mod.bar"));
    AmbiguousSymbol ambiguousSymbol = (AmbiguousSymbol) symbols.get("bar");
    assertThat(ambiguousSymbol.alternatives()).extracting(Symbol::kind).containsExactly(Kind.FUNCTION, Kind.FUNCTION);

  }

  @Test
  public void overloaded_functions() {
    Symbol map = TypeShed.builtinSymbols().get("map");
    assertThat(map.is(Kind.AMBIGUOUS)).isTrue();
    assertThat(((SymbolImpl) map).validForPythonVersions()).containsExactlyInAnyOrder("27", "35", "36", "37", "38", "39", "310");
    ClassSymbol python3Symbol = (ClassSymbol) ((AmbiguousSymbol) map).alternatives().stream().filter(s -> s.is(Kind.CLASS)).findFirst().get();
    assertThat(((ClassSymbolImpl) python3Symbol).validForPythonVersions()).containsExactlyInAnyOrder("35", "36", "37", "38", "39", "310");
    Set<Symbol> python2Symbols = ((AmbiguousSymbol) map).alternatives().stream().filter(s -> s.is(Kind.FUNCTION)).collect(Collectors.toSet());
    for (Symbol alternative : python2Symbols) {
      assertThat(alternative.is(Kind.FUNCTION)).isTrue();
      assertThat(((FunctionSymbolImpl) alternative).validForPythonVersions()).containsExactly("27");
    }
  }

  @Test
  public void pythonVersions() {
    // unknown version
    Symbol range = TypeShed.builtinSymbols().get("range");
    assertThat(((SymbolImpl) range).validForPythonVersions()).containsExactlyInAnyOrder("27", "35", "36", "37", "38", "39", "310");
    assertThat(range.kind()).isEqualTo(Kind.AMBIGUOUS);

    // python 2
    setPythonVersions(PythonVersionUtils.fromString("2.7"));
    range = TypeShed.builtinSymbols().get("range");
    assertThat(((SymbolImpl) range).validForPythonVersions()).containsExactlyInAnyOrder("27");
    assertThat(range.kind()).isEqualTo(Kind.FUNCTION);

    // python 3
    setPythonVersions(PythonVersionUtils.fromString("3.8"));
    range = TypeShed.builtinSymbols().get("range");
    assertThat(((SymbolImpl) range).validForPythonVersions()).containsExactlyInAnyOrder("35", "36", "37", "38", "39", "310");
    assertThat(range.kind()).isEqualTo(Kind.CLASS);

    setPythonVersions(PythonVersionUtils.fromString("3.10"));
    ClassSymbol intSymbol = TypeShed.typeShedClass("int");
    assertThat(intSymbol.resolveMember("bit_count")).isNotEmpty();

    setPythonVersions(PythonVersionUtils.allVersions());
  }

  @Test
  public void symbolWithFQN_should_be_consistent() {
    // smtplib imports typing.Sequence only in Python3, hence typing.Sequence has kind CLASS
    symbolsForModule("smtplib");
    Symbol sequence = TypeShed.symbolWithFQN("typing.Sequence");
    assertThat(sequence.kind()).isEqualTo(Kind.AMBIGUOUS);
    Map<String, Symbol> typing = symbolsForModule("typing");
    assertThat(sequence).isSameAs(typing.get("Sequence"));
  }

  private static SymbolsProtos.ModuleSymbol moduleSymbol(String protobuf) throws TextFormat.ParseException {
    SymbolsProtos.ModuleSymbol.Builder builder = SymbolsProtos.ModuleSymbol.newBuilder();
    TextFormat.merge(protobuf, builder);
    return builder.build();
  }

  @Test
  public void variables_from_protobuf() throws TextFormat.ParseException {
    SymbolsProtos.ModuleSymbol moduleSymbol = moduleSymbol(
        "fully_qualified_name: \"mod\"\n" +
        "vars {\n" +
        "  name: \"foo\"\n" +
        "  fully_qualified_name: \"mod.foo\"\n" +
        "  type_annotation {\n" +
        "    pretty_printed_name: \"builtins.str\"\n" +
        "    fully_qualified_name: \"builtins.str\"\n" +
        "  }\n" +
        "}\n" +
        "vars {\n" +
        "  name: \"bar\"\n" +
        "  fully_qualified_name: \"mod.bar\"\n" +
        "}\n");
    Map<String, Symbol> symbols = TypeShed.getSymbolsFromProtobufModule(moduleSymbol);
    assertThat(symbols.values()).extracting(Symbol::kind, Symbol::fullyQualifiedName, Symbol::annotatedTypeName)
      .containsExactlyInAnyOrder(tuple(Kind.OTHER, "mod.foo", "str"), tuple(Kind.OTHER, "mod.bar", null));
  }

  @Test
  public void stubFilesSymbols_should_not_contain_ambiguous_symbols_of_classes() {
    // populate typeshed symbols cache by importing socket module
    symbolsForModule("socket");
    Collection<Symbol> symbols = TypeShed.stubFilesSymbols();
    assertThat(symbols).noneMatch(s -> s.is(Kind.AMBIGUOUS) && ((AmbiguousSymbol) s).alternatives().stream().allMatch(a -> a.is(Kind.CLASS)));
  }

  @Test
  public void symbol_from_submodule_access() {
    Map<String, Symbol> os = symbolsForModule("os");
    SymbolImpl path = (SymbolImpl) os.get("path");
    Symbol samefile = path.getChildrenSymbolByName().get("samefile");
    assertThat(samefile).isNotNull();
    assertThat(samefile.fullyQualifiedName()).isEqualTo("os.path.samefile");

    Map<String, Symbol> osPath = symbolsForModule("os.path");
    Symbol samefileFromSubModule = osPath.get("samefile");
    assertThat(samefileFromSubModule).isSameAs(samefile);
  }

  @Test
  public void typeshed_private_modules_should_not_affect_fqn()  {
    Map<String, Symbol> socketModule = symbolsForModule("socket");
    ClassSymbol socket = (ClassSymbol) TypeShed.disambiguateWithLatestPythonSymbol(((AmbiguousSymbol) socketModule.get("socket")).alternatives());
    assertThat(socket.declaredMembers()).extracting(Symbol::name, Symbol::fullyQualifiedName).contains(tuple("connect", "socket.socket.connect"));
    assertThat(socket.superClasses()).extracting(Symbol::fullyQualifiedName).containsExactly("object");
  }

  @Test
  public void overloaded_function_alias_has_function_annotated_type()  {
    Map<String, Symbol> gettextModule = symbolsForModule("gettext");
    Symbol translation = gettextModule.get("translation");
    Symbol catalog = gettextModule.get("Catalog");
    assertThat(translation.kind()).isEqualTo(Kind.AMBIGUOUS);
    assertThat(catalog.annotatedTypeName()).isEqualTo("function");
  }

  @Test
  public void stubFilesSymbols_third_party_symbols_should_not_be_null() {
    // six modules contain ambiguous symbols that only contain class symbols
    // however third party symbols don't have validForPythonVersions field set
    symbolsForModule("six");
    assertThat(TypeShed.stubFilesSymbols()).doesNotContainNull();
  }

  @Test
  public void modules_whose_name_differ_by_capitalization_only() {
    // Python 2, import SocketServer
    setPythonVersions(PythonVersionUtils.fromString("2.7"));
    Map<String, Symbol> socketServer = symbolsForModule("SocketServer");
    assertThat(socketServer).isNotEmpty();
    SymbolImpl baseServer = (SymbolImpl) socketServer.get("BaseServer");
    assertThat(baseServer.validForPythonVersions()).containsExactlyInAnyOrder("27");

    // Python 2, import socketserver
    socketServer = symbolsForModule("socketserver");
    assertThat(socketServer).isEmpty();

    // Python 3, import SocketServer
    setPythonVersions(PythonVersionUtils.fromString("3.10"));
    socketServer = symbolsForModule("SocketServer");
    assertThat(socketServer).isEmpty();

    // Python 3, import socketserver
    socketServer = symbolsForModule("socketserver");
    assertThat(socketServer).isNotEmpty();
    baseServer = (SymbolImpl) socketServer.get("BaseServer");
    assertThat(baseServer.validForPythonVersions()).containsExactlyInAnyOrder("35", "36", "37", "38", "39", "310");

    // Unknown Python version, import SocketServer
    // in this case we assume Python2 module is imported
    setPythonVersions();
    socketServer = symbolsForModule("SocketServer");
    assertThat(socketServer).isNotEmpty();
    baseServer = (SymbolImpl) socketServer.get("BaseServer");
    assertThat(baseServer.validForPythonVersions()).containsExactlyInAnyOrder("27");

    // Unknown Python version, import socketserver
    // in this case we assume Python3 module is imported
    socketServer = symbolsForModule("socketserver");
    assertThat(socketServer).isNotEmpty();
    baseServer = (SymbolImpl) socketServer.get("BaseServer");
    assertThat(baseServer.validForPythonVersions()).containsExactlyInAnyOrder("35", "36", "37", "38", "39", "310");
  }
}
