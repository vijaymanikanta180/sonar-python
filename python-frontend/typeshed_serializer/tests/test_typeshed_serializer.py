#
# SonarQube Python Plugin
# Copyright (C) 2011-2022 SonarSource SA
# mailto:info AT sonarsource DOT com
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

from unittest.mock import Mock

from serializer import typeshed_serializer, symbols, symbols_merger
import os


def test_build_mypy_model(typeshed_stdlib):
    assert typeshed_stdlib is not None


def test_serialize_typeshed_stdlib(typeshed_stdlib):
    typeshed_serializer.walk_typeshed_stdlib = Mock(return_value=(typeshed_stdlib, set()))
    symbols.save_module = Mock()
    typeshed_serializer.serialize_typeshed_stdlib()
    assert typeshed_serializer.walk_typeshed_stdlib.call_count == 1
    assert symbols.save_module.call_count == len(typeshed_stdlib.files)


def test_serialize_custom_stubs(typeshed_custom_stubs):
    typeshed_serializer.walk_custom_stubs = Mock(return_value=(typeshed_custom_stubs, set()))
    symbols.save_module = Mock()
    typeshed_serializer.serialize_custom_stubs()
    assert typeshed_serializer.walk_custom_stubs.call_count == 1
    # Not every files from "typeshed_custom_stubs" build are serialized, as some are builtins
    assert symbols.save_module.call_count == 50


def test_all_third_parties_are_serialized(typeshed_third_parties):
    stub_modules = set()
    for stub_folder in typeshed_serializer.THIRD_PARTIES_STUBS:
        stub_path = os.path.join(typeshed_serializer.CURRENT_PATH, typeshed_serializer.STUBS_PATH, stub_folder)
        _, dirs, files = os.walk(stub_path).__next__()
        stub_modules = stub_modules.union([dir for dir in dirs if not dir.startswith("@")])
        stub_modules = stub_modules.union([file.replace(".pyi", "") for file in files if file.endswith(".pyi")])
    for third_party_stub in stub_modules:
        assert third_party_stub in typeshed_third_parties


def test_serialize_typeshed_stdlib_multiple_python_version():
    typeshed_serializer.serialize_typeshed_stdlib = Mock()
    typeshed_serializer.serialize_typeshed_stdlib_multiple_python_version()
    assert typeshed_serializer.serialize_typeshed_stdlib.call_count == len(range(5, 10))
    versions_called = set()
    for call in typeshed_serializer.serialize_typeshed_stdlib.mock_calls:
        versions_called.add(call.args[1])
    assert versions_called == {(3, 5), (3, 6), (3, 7), (3, 8), (3, 9)}


def test_save_merged_symbols():
    merged_module_symbol = symbols.MergedModuleSymbol('abc', {}, {}, {}, {})
    symbols_merger.merge_multiple_python_versions = Mock(return_value={'abc': merged_module_symbol})
    symbols.save_module = Mock()
    typeshed_serializer.save_merged_symbols()
    assert symbols.save_module.call_count == 1
    assert symbols.save_module.mock_calls[0].args[0] == merged_module_symbol
