/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.saradabar.cpadcustomizetool.data.service;
public interface IDeviceOwnerService extends android.os.IInterface
{
  /** Default implementation for IDeviceOwnerService. */
  public static class Default implements com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService
  {
    @Override public boolean isDeviceOwnerApp() throws android.os.RemoteException
    {
      return false;
    }
    @Override public void setUninstallBlocked(java.lang.String packageName, boolean uninstallBlocked) throws android.os.RemoteException
    {
    }
    @Override public boolean isUninstallBlocked(java.lang.String packageName) throws android.os.RemoteException
    {
      return false;
    }
    @Override public boolean tryInstallPackages(java.lang.String packageName, java.util.List<android.net.Uri> uriList) throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService interface,
     * generating a proxy if needed.
     */
    public static com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService))) {
        return ((com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService)iin);
      }
      return new com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_isDeviceOwnerApp:
        {
          boolean _result = this.isDeviceOwnerApp();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_setUninstallBlocked:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          boolean _arg1;
          _arg1 = (0!=data.readInt());
          this.setUninstallBlocked(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_isUninstallBlocked:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          boolean _result = this.isUninstallBlocked(_arg0);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_tryInstallPackages:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.util.List<android.net.Uri> _arg1;
          _arg1 = data.createTypedArrayList(android.net.Uri.CREATOR);
          boolean _result = this.tryInstallPackages(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public boolean isDeviceOwnerApp() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isDeviceOwnerApp, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public void setUninstallBlocked(java.lang.String packageName, boolean uninstallBlocked) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          _data.writeInt(((uninstallBlocked)?(1):(0)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_setUninstallBlocked, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public boolean isUninstallBlocked(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isUninstallBlocked, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      @Override public boolean tryInstallPackages(java.lang.String packageName, java.util.List<android.net.Uri> uriList) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          _Parcel.writeTypedList(_data, uriList, 0);
          boolean _status = mRemote.transact(Stub.TRANSACTION_tryInstallPackages, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_isDeviceOwnerApp = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_setUninstallBlocked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_isUninstallBlocked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_tryInstallPackages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
  }
  public static final java.lang.String DESCRIPTOR = "com.saradabar.cpadcustomizetool.data.service.IDeviceOwnerService";
  public boolean isDeviceOwnerApp() throws android.os.RemoteException;
  public void setUninstallBlocked(java.lang.String packageName, boolean uninstallBlocked) throws android.os.RemoteException;
  public boolean isUninstallBlocked(java.lang.String packageName) throws android.os.RemoteException;
  public boolean tryInstallPackages(java.lang.String packageName, java.util.List<android.net.Uri> uriList) throws android.os.RemoteException;
  /** @hide */
  static class _Parcel {
    static private <T> T readTypedObject(
        android.os.Parcel parcel,
        android.os.Parcelable.Creator<T> c) {
      if (parcel.readInt() != 0) {
          return c.createFromParcel(parcel);
      } else {
          return null;
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedObject(
        android.os.Parcel parcel, T value, int parcelableFlags) {
      if (value != null) {
        parcel.writeInt(1);
        value.writeToParcel(parcel, parcelableFlags);
      } else {
        parcel.writeInt(0);
      }
    }
    static private <T extends android.os.Parcelable> void writeTypedList(
        android.os.Parcel parcel, java.util.List<T> value, int parcelableFlags) {
      if (value == null) {
        parcel.writeInt(-1);
      } else {
        int N = value.size();
        int i = 0;
        parcel.writeInt(N);
        while (i < N) {
    writeTypedObject(parcel, value.get(i), parcelableFlags);
          i++;
        }
      }
    }
  }
}
