// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: extensions.proto

package io.particle.firmwareprotos.ctrl;

public final class Extensions {
  private Extensions() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
    registry.add(io.particle.firmwareprotos.ctrl.Extensions.typeId);
    registry.add(io.particle.firmwareprotos.ctrl.Extensions.intValue);
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public static final int TYPE_ID_FIELD_NUMBER = 50001;
  /**
   * <code>extend .google.protobuf.MessageOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.MessageOptions,
      java.lang.Integer> typeId = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Integer.class,
        null);
  public static final int INT_VALUE_FIELD_NUMBER = 50002;
  /**
   * <code>extend .google.protobuf.EnumValueOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.EnumValueOptions,
      java.lang.Integer> intValue = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Integer.class,
        null);

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020extensions.proto\032 google/protobuf/desc" +
      "riptor.proto:2\n\007type_id\022\037.google.protobu" +
      "f.MessageOptions\030\321\206\003 \001(\005:6\n\tint_value\022!." +
      "google.protobuf.EnumValueOptions\030\322\206\003 \001(\005" +
      "B!\n\037io.particle.firmwareprotos.ctrlb\006pro" +
      "to3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.DescriptorProtos.getDescriptor(),
        });
    typeId.internalInit(descriptor.getExtensions().get(0));
    intValue.internalInit(descriptor.getExtensions().get(1));
    com.google.protobuf.DescriptorProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
