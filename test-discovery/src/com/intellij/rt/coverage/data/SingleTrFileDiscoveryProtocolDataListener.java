/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.data;

import java.io.*;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SingleTrFileDiscoveryProtocolDataListener extends TestDiscoveryProtocolDataListener {
  @SuppressWarnings("WeakerAccess")
  public static final String TRACE_FILE = "org.jetbrains.instrumentation.trace.file";
  @SuppressWarnings("WeakerAccess")
  public static final String BUFFER_SIZE = "org.jetbrains.instrumentation.trace.file.buffer.size";
  @SuppressWarnings("WeakerAccess")
  public static final String FILE_VERSION = "org.jetbrains.instrumentation.trace.file.version";

  public static final byte HEADER_START = 0x49; // "I"
  public static final byte[] HEADER_TAIL = new byte[]{0x4a, 0x54, 0x43}; // "JTC"

  private static final int DEFAULT_VERSION = 0x2;

  private final DataOutputStream myStream;
  private final NameEnumerator.Incremental myNameEnumerator;


  public SingleTrFileDiscoveryProtocolDataListener() throws Exception {
    super(Byte.parseByte(System.getProperty(FILE_VERSION, String.valueOf(DEFAULT_VERSION))));
    final File myTraceFile = getCanonicalFile(new File(System.getProperty(TRACE_FILE, "td.ijtc")));
    int bufferSize = Integer.parseInt(System.getProperty(BUFFER_SIZE, "32768"));
    //noinspection ResultOfMethodCallIgnored
    myTraceFile.getParentFile().mkdirs();
    myStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(myTraceFile), bufferSize));
    myNameEnumerator = new NameEnumerator.Incremental();
    start(this.myStream);
  }

  // For tests
  SingleTrFileDiscoveryProtocolDataListener(DataOutputStream stream, int version) throws Exception {
    super((byte) version);
    myStream = stream;
    myNameEnumerator = new NameEnumerator.Incremental();
    start(myStream);
  }

  public synchronized void testFinished(String className, String methodName, Map<Integer, boolean[]> classToVisitedMethods, Map<Integer, int[]> classToMethodNames) throws IOException {
    writeTestFinished(myStream, className, methodName, classToVisitedMethods, classToMethodNames);
  }

  public synchronized void testsFinished() throws IOException {
    try {
      writeDictionaryIncrementIfNeeded(myStream);
      finish(myStream);
    } finally {
      myStream.close();
    }
  }

  public NameEnumerator.Incremental getNameEnumerator() {
    return myNameEnumerator;
  }

  public synchronized void addMetadata(Map<String, String> metadata) throws IOException {
    writeMetadata(myStream, metadata);
  }

  public void addClassMetadata(List<ClassMetadata> metadata) throws IOException {
    writeClassMetadata(myStream, metadata);
  }

  protected synchronized void start(DataOutput output) throws IOException {
    output.writeByte(HEADER_START);
    output.write(HEADER_TAIL);
    super.start(output);
  }


  private static File getCanonicalFile(File file) {
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      return file.getAbsoluteFile();
    }
  }
}
