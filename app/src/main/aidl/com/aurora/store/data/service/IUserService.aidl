package com.aurora.store.data.service;

interface IUserService {
    boolean isInstallPackages(String str, in List<Uri> uriList) = 21;
}