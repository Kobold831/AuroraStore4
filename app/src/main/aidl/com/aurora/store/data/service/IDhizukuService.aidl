package com.aurora.store.data.service;

interface IDhizukuService {
    boolean tryInstallPackages(in List<String> listFiles) = 21;
}