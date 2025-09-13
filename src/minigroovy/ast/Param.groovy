package minigroovy.ast

class Param {
    final String name
    final String typeName
    Param(String name, String typeName) { this.name = name; this.typeName = typeName }
    String toString() { "${name}: ${typeName}" }
}
