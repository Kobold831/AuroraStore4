/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.saradabar.cpadcustomizetool.data.service;
public interface IDhizukuService extends android.os.IInterface
{
  /** Default implementation for IDhizukuService. */
  public static class Default implements com.saradabar.cpadcustomizetool.data.service.IDhizukuService
  {
    @Override public void setUninstallBlocked(java.lang.String packageName, boolean uninstallBlocked) throws android.os.RemoteException
    {
    }
    @Override public boolean isUninstallBlocked(java.lang.String packageName) throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.saradabar.cpadcustomizetool.data.service.IDhizukuService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.saradabar.cpadcustomizetool.data.service.IDhizukuService interface,
     * generating a proxy if needed.
     */
    public static com.saradabar.cpadcustomizetool.data.service.IDhizukuService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.saradabar.cpadcustomizetool.data.service.IDhizukuService))) {
        return ((com.saradabar.cpadcustomizetool.data.service.IDhizukuService)iin);
      }
      return new com.saradabar.cpadcustomizetool.data.service.IDhizukuService.Stub.Proxy(obj);
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
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.saradabar.cpadcustomizetool.data.service.IDhizukuService
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
    }
    static final int TRANSACTION_setUninstallBlocked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_isUninstallBlocked = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  public static final java.lang.String DESCRIPTOR = "com.saradabar.cpadcustomizetool.data.service.IDhizukuService";
  public void setUninstallBlocked(java.lang.String packageName, boolean uninstallBlocked) throws android.os.RemoteException;
  public boolean isUninstallBlocked(java.lang.String packageName) throws android.os.RemoteException;
}
