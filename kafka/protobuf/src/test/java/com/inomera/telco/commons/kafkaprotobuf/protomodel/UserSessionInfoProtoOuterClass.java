package com.inomera.telco.commons.kafkaprotobuf.protomodel;

// @@protoc_insertion_point(outer_class_scope)
public final class UserSessionInfoProtoOuterClass {
    private UserSessionInfoProtoOuterClass() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        UserSessionInfoProtoOuterClass.registerAllExtensions(((com.google.protobuf.ExtensionRegistryLite) (registry)));
    }

    static final com.google.protobuf.Descriptors.Descriptor internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_fieldAccessorTable;

    static final com.google.protobuf.Descriptors.Descriptor internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_descriptor;

    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return UserSessionInfoProtoOuterClass.descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        String[] descriptorData = new String[]{"\n:com/inomera/muud/domain/session/UserSe" + ((((((((((((((("ssionInfoProto.proto\u0012\u001fcom.inomera.muud.d" + "omain.session\"Î\u0002\n\u0017AuthenticationInfoProt") + "o\u0012\u000e\n\u0006result\u0018\u0001 \u0001(\u0005\u0012\u0010\n\bfromAvea\u0018\u0002 \u0001(\b\u0012\u000e\n\u0006m") + "sisdn\u0018\u0003 \u0001(\t\u0012\u0013\n\u000bssoUniqueId\u0018\u0004 \u0001(\t\u0012\u0013\n\u000bssoU") + "sername\u0018\u0005 \u0001(\t\u0012\u0015\n\rmtsCustomerId\u0018\u0006 \u0001(\t\u0012\u000e\n\u0006") + "adslNo\u0018\u0007 \u0001(\t\u0012\u000f\n\u0007fromSSO\u0018\b \u0001(\b\u0012\u0015\n\rfromSSO") + "Msisdn\u0018\t \u0001(\b\u0012\u0010\n\buserType\u0018\n \u0001(\t\u0012\r\n\u0005token\u0018") + "\u000b \u0001(\t\u0012\u0011\n\tfirstName\u0018\f \u0001(\t\u0012\u0010\n\blastName\u0018\r \u0001") + "(\t\u0012\u0016\n\u000eserviceMessage\u0018\u000e \u0001(\t\u0012\u000f\n\u0007premium\u0018\u000f ") + "\u0001(\b\u0012\u0019\n\u0011fromMobileConnect\u0018\u0010 \u0001(\b\"è\u0001\n\u0014UserS") + "essionInfoProto\u0012\u000e\n\u0006userId\u0018\u0001 \u0001(\t\u0012\u0012\n\nsessi") + "onKey\u0018\u0002 \u0001(\t\u0012\u001a\n\u0012lastActivationTime\u0018\u0003 \u0001(\u0003\u0012") + "T\n\u0012authenticationInfo\u0018\u0004 \u0001(\u000b28.com.inomer") + "a.muud.domain.session.AuthenticationInfo") + "Proto\u0012\u0014\n\frequestCount\u0018\u0005 \u0001(\u0005\u0012\u0010\n\bdeviceId\u0018") + "\u0006 \u0001(\t\u0012\u0012\n\ndeviceName\u0018\u0007 \u0001(\tB\u0002P\u0001b\u0006proto3")};
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner = new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
            public com.google.protobuf.ExtensionRegistry assignDescriptors(com.google.protobuf.Descriptors.FileDescriptor root) {
                UserSessionInfoProtoOuterClass.descriptor = root;
                return null;
            }
        };
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[]{}, assigner);
        internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_descriptor = getDescriptor().getMessageTypes().get(0);
        internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_com_inomera_muud_domain_session_AuthenticationInfoProto_descriptor, new String[]{"Result", "FromAvea", "Msisdn", "SsoUniqueId", "SsoUsername", "MtsCustomerId", "AdslNo", "FromSSO", "FromSSOMsisdn", "UserType", "Token", "FirstName", "LastName", "ServiceMessage", "Premium", "FromMobileConnect"});
        internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_descriptor = getDescriptor().getMessageTypes().get(1);
        internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(internal_static_com_inomera_muud_domain_session_UserSessionInfoProto_descriptor, new String[]{"UserId", "SessionKey", "LastActivationTime", "AuthenticationInfo", "RequestCount", "DeviceId", "DeviceName"});
    }
}
