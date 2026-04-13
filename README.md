# ntrip-client

[![Maven Central](https://img.shields.io/maven-central/v/io.github.christian-sidak/ntrip-client)](https://central.sonatype.com/artifact/io.github.christian-sidak/ntrip-client)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Lightweight Java NTRIP client for receiving GNSS RTK correction data. Zero runtime dependencies. Java 11+.

## Install

### Maven

```xml
<dependency>
    <groupId>io.github.christian-sidak</groupId>
    <artifactId>ntrip-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.christian-sidak:ntrip-client:0.1.0'
```

## Usage

### List available mountpoints

```java
import io.github.christian-sidak.ntrip.client.NtripClient;
import io.github.christian-sidak.ntrip.sourcetable.Sourcetable;

NtripClient client = new NtripClient.Builder("rtk2go.com", 2101).build();

Sourcetable table = client.getSourcetable();
for (var stream : table.getStreams()) {
    System.out.println(stream.getMountpoint() + " - " + stream.getFormat()
        + " (" + stream.getCountry() + ")");
}
```

### Stream RTCM corrections

```java
import io.github.christian-sidak.ntrip.client.NtripClient;
import io.github.christian-sidak.ntrip.protocol.NtripVersion;

NtripClient client = new NtripClient.Builder("rtk2go.com", 2101)
    .credentials("user@example.com", "none")
    .version(NtripVersion.V1)
    .build();

// Decoded RTCM frames
client.stream("MY_MOUNT", frame -> {
    System.out.println("RTCM type " + frame.getMessageType()
        + ", " + frame.getPayload().length + " bytes");
});
```

### Stream raw bytes

```java
// Raw correction data (forward to a GNSS receiver)
client.streamRaw("MY_MOUNT", chunk -> {
    serialPort.write(chunk);
});
```

### VRS (Virtual Reference Station)

For network RTK mountpoints that require your position:

```java
String gga = "$GPGGA,115739.00,4158.8441,N,09147.4417,W,1,08,0.9,255.7,M,-32.0,M,,*6E";

NtripClient client = new NtripClient.Builder("caster.example.com", 2101)
    .credentials("user", "pass")
    .build();

// Start streaming in a separate thread
new Thread(() -> {
    try {
        client.stream("VRS_MOUNT", gga, frame -> {
            System.out.println(frame);
        });
    } catch (IOException e) {
        e.printStackTrace();
    }
}).start();

// Update position periodically
client.sendGga("$GPGGA,115740.00,4158.8442,N,09147.4418,W,1,08,0.9,255.7,M,-32.0,M,,*6F");
```

### Use the RTCM decoder standalone

```java
import io.github.christian-sidak.ntrip.rtcm.RtcmDecoder;

RtcmDecoder decoder = new RtcmDecoder(inputStream);
decoder.decode(frame -> {
    System.out.println("Message type: " + frame.getMessageType());
});
```

## API

### `NtripClient`

Built with `NtripClient.Builder`:

| Method | Description |
|--------|-------------|
| `Builder(host, port)` | Create builder with caster address |
| `.credentials(user, pass)` | Set Basic auth credentials |
| `.version(NtripVersion)` | Set protocol version (V1 or V2, default V1) |
| `.connectTimeout(ms)` | Connection timeout (default 10s) |
| `.readTimeout(ms)` | Read timeout (default 30s) |

| Method | Description |
|--------|-------------|
| `getSourcetable()` | Fetch and parse the caster's sourcetable |
| `stream(mountpoint, consumer)` | Stream decoded RTCM frames |
| `stream(mountpoint, gga, consumer)` | Stream with GGA for VRS |
| `streamRaw(mountpoint, consumer)` | Stream raw byte chunks |
| `sendGga(sentence)` | Send updated GGA on active connection |
| `close()` | Disconnect |

### `RtcmDecoder`

Streaming decoder for RTCM 3.x binary frames. Scans for 0xD3 preamble, validates CRC-24Q, extracts message type and payload.

### `Sourcetable`

Parsed sourcetable with `getStreams()`, `getCasters()`, `getNetworks()`, `findStream(name)`, and `getRtcm3Streams()`.

## NTRIP Protocol Support

| Feature | Status |
|---------|--------|
| NTRIP v1 (ICY 200 OK) | Supported |
| NTRIP v2 (HTTP/1.1 chunked) | Supported |
| Basic authentication | Supported |
| RTCM 3.x frame decoding | Supported |
| CRC-24Q validation | Supported |
| Sourcetable parsing (STR/CAS/NET) | Supported |
| VRS (GGA position updates) | Supported |

## License

MIT
