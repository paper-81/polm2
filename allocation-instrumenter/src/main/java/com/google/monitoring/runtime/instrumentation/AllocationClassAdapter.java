/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.monitoring.runtime.instrumentation;

import java.util.HashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * Instruments bytecodes that allocate heap memory to call a recording hook.
 * A <code>ClassVisitor</code> that processes methods with a
 * <code>AllocationMethodAdapter</code> to instrument heap allocations.
 *
 * @author jeremymanson@google.com (Jeremy Manson)
 * @author fischman@google.com (Ami Fischman) (Original Author)
 */
class AllocationClassAdapter extends ClassVisitor {
  private final HashMap<String, HashMap<Integer, InstRequest>> instRequests;

  public AllocationClassAdapter(ClassVisitor cv,
          HashMap<String, HashMap<Integer, InstRequest>> instRequests) {
    super(Opcodes.ASM5, cv);
    this.instRequests = instRequests;
  }

  /**
   * For each method in the class being instrumented, <code>visitMethod</code>
   * is called and the returned MethodVisitor is used to visit the method.
   * Note that a new MethodVisitor is constructed for each method.
   */
  @Override
  public MethodVisitor visitMethod(int access, String base, String desc,
      String signature, String[] exceptions) {
    MethodVisitor mv =
      cv.visitMethod(access, base, desc, signature, exceptions);

    // Note: checking if method should be instrumented or not.
    if (mv != null && instRequests.containsKey(base)) {
      System.out.println("Visiting method " + String.format("%s %s %s", base, desc, signature)); // DEBUG
      // We need to compute stackmaps (see
      // AllocationInstrumenter#instrument).  This can't really be
      // done for old bytecode that contains JSR and RET instructions.
      // So, we remove JSRs and RETs.
      JSRInlinerAdapter jsria = new JSRInlinerAdapter(
          mv, access, base, desc, signature, exceptions);
      AllocationMethodAdapter aimv =
        new AllocationMethodAdapter(jsria, instRequests.get(base));
      LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, aimv);
      aimv.lvs = lvs;
      mv = lvs;
    }
    return mv;
  }
}
