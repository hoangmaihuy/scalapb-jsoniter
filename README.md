# scalapb-jsoniter

JSON/Protobuf converters for [ScalaPB](https://scalapb.github.io/) using [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala).

The structure of this project is hugely inspired by [scalapb-circe](https://github.com/scalapb-json/scalapb-circe) and [scalapb-json4s](https://github.com/scalapb/scalapb-json4s).

## Dependency

Include in your `build.sbt` file

```scala
libraryDependencies += "io.github.scalapb-json" %% "scalapb-jsoniter" % "0.1.0-SNAPSHOT"
```

For Scala.js or Scala Native

```scala
libraryDependencies += "io.github.scalapb-json" %%% "scalapb-jsoniter" % "0.1.0-SNAPSHOT"
```

## Usage

### JsonFormat

There are two functions you can use directly to serialize/deserialize your messages:

```scala
import scalapb_jsoniter.JsonFormat

JsonFormat.toJsonString(msg) // returns String
JsonFormat.fromJsonString[MyMessage](str) // returns MyMessage
```

### Printer and Parser

For more control, create `Printer` and `Parser` instances with custom settings:

```scala
import scalapb_jsoniter.{Printer, Parser}

val printer = new Printer(
  includingDefaultValueFields = true,
  preservingProtoFieldNames = true,
  formattingLongAsNumber = true,
  formattingEnumsAsNumber = true,
  formattingMapEntriesAsKeyValuePairs = true
)

printer.print(msg) // returns String
```

```scala
val parser = new Parser(
  preservingProtoFieldNames = true,
  mapEntriesAsKeyValuePairs = true
)

parser.fromJsonString[MyMessage](str) // returns MyMessage
```

### Streaming API

You can also write/read directly to jsoniter-scala's `JsonWriter`/`JsonReader`:

```scala
import com.github.plokhotnyuk.jsoniter_scala.core.*
import scalapb_jsoniter.{Printer, Parser}

val printer = new Printer()
val parser = new Parser()

// Write to JsonWriter
printer.toJson(msg, out)

// Read from JsonReader
parser.fromJson[MyMessage](in)
```

### Implicit jsoniter-scala Codecs

Import codecs to get implicit `JsonValueCodec` instances for any `GeneratedMessage` or `GeneratedEnum`:

```scala
import com.github.plokhotnyuk.jsoniter_scala.core.*
import scalapb_jsoniter.codec.*

writeToString(Guitar(42)) // returns {"numberOfStrings":42}
readFromString[Guitar]("""{"numberOfStrings": 42}""") // returns Guitar(42)
```

You can define an implicit `Printer` and/or `Parser` to control printing and parsing settings.
For example, to include default values in JSON:

```scala
import com.github.plokhotnyuk.jsoniter_scala.core.*
import scalapb_jsoniter.codec.*
import scalapb_jsoniter.Printer

implicit val p: Printer = new Printer(includingDefaultValueFields = true)

writeToString(Guitar(0)) // returns {"numberOfStrings":0}
```

### google.protobuf.Any

To serialize/deserialize `google.protobuf.Any` messages, register the relevant message companions in a `TypeRegistry`:

```scala
import scalapb_json.TypeRegistry
import scalapb_jsoniter.{Printer, Parser}

val typeRegistry = TypeRegistry.empty
  .addMessageByCompanion(MyMessage.companion)

val printer = new Printer(typeRegistry = typeRegistry)
val parser = new Parser(typeRegistry = typeRegistry)
```

## Supported Types

All standard protobuf types are supported, including well-known types:

- Primitive types (int32, int64, uint32, uint64, float, double, bool, string, bytes)
- Enums
- Nested messages
- Repeated fields and maps
- Oneof fields
- `google.protobuf.Timestamp`
- `google.protobuf.Duration`
- `google.protobuf.FieldMask`
- `google.protobuf.Struct`, `Value`, `ListValue`
- `google.protobuf.Any`
- Wrapper types (`DoubleValue`, `FloatValue`, `Int32Value`, `Int64Value`, `UInt32Value`, `UInt64Value`, `BoolValue`, `StringValue`, `BytesValue`)

## Credits

- https://github.com/scalapb-json/scalapb-circe
- https://github.com/scalapb/scalapb-json4s
- https://github.com/plokhotnyuk/jsoniter-scala
