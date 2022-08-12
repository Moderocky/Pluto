Pluto
=====

### Opus #21

A low-memory serialiser system that prioritises speed.

## Introduction

Pluto aims to provide object serialisation (marshalling and unmarshalling data from a format that can be written to the disk)
that is both memory-efficient (e.g. not building massive strings) and as fast as possible.

In order to make this work, Pluto must compromise.
Firstly, the design is restricted to systems where the potential types of objects are known.
Secondly, the design favours a more loosely-defined object (without final fields.)
These are not significant restrictions.

With these restrictions in mind, Pluto is able to create what is potentially the fastest serialiser possible (aside from dumping an object's heap memory representation.)

When the serialisers are set up (ideally at the start of a program's run, or even during compilation)
Pluto will compile a new serialiser class for each required object type.

A simple class like
```java
class Person {
    String name;
    int age;
}
```
will have an equally-simple serialiser written.
```java 
void run(Person person, DataOutputStream stream) {
    stream.writeUTF(person.name);
    stream.writeInt(person.age);
}

void run(Person person, DataInputStream stream) {
    person.name = stream.readUTF();
    person.age = stream.readInt();
}
```

This is potentially the fastest serialiser possible.
All field access is direct, with minimal overhead. No reflection is involved.

The serialiser can be configured to read or ignore non-public fields.

Ideally, this serialiser should be written in advance. \
Marshalling and unmarshalling the data is fast and trivial,
but compiling the bytecode and inserting the new class takes longer.

The class is inserted as a nest-mate of the target so that it can see non-public fields.

Serialisers support non-trivial object types under certain conditions:
1. Regular serialisers cannot create new objects, so the field on the deserialiser target must be set already.
2. The object must be accessible for reading and writing.

## Unsafe Serialisers

For situations where more delicate serialisation is required (such as altering final fields that should have been assigned in the constructor)
Pluto provides unsafe serialisers.

An unsafe serialiser has equivalent speed to the regular serialiser, but is naturally more risky to use.
These serialisers copy the data to and from the stream directly into the VM's heap memory.

A simple class like
```java
class Person {
    String name;
    int age;
}
```
will have a serialiser written like so.
```java 
void run(Person person, DataOutputStream stream) {
    stream.writeUTF(unsafe.getObject(person, name));
    stream.writeInt(unsafe.getInt(person, age));
}

void run(Person person, DataInputStream stream) {
    unsafe.putObject(person, name, stream.readUTF());
    unsafe.putInt(person, age, stream.readInt());
}
```
