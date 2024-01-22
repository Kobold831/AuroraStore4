package com.aurora.store.data.service;

interface IDhizukuService {
    boolean tryInstallPackages(String str, in List<Uri> uriList) = 21;
}