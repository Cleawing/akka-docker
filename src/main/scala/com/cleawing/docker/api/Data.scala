package com.cleawing.docker.api

import org.json4s._
import org.json4s.jackson.Serialization

object Data {

  // Object responses
  case class Pong(msg: String)
  case class Version
  (
    Version: String,
    Os: String,
    KernelVersion: String,
    GoVersion: String,
    GitCommit: String,
    Arch: String,
    ApiVersion: String
  )

  case class Info
  (
    ID: String,
    Containers: Int,
    Images: Int,
    Driver: String,
    DriverStatus: Seq[Seq[String]],
    MemoryLimit: Boolean,
    SwapLimit: Boolean,
    CpuCfsPeriod: Boolean,
    CpuCfsQuota: Boolean,
    IPv4Forwarding: Boolean,
    Debug: Boolean,
    NFd: Int,
    OomKillDisable: Boolean,
    NGoroutines: Int,
    SystemTime: String,
    ExecutionDriver: String,
    LoggingDriver: String,
    NEventsListener: Int,
    KernelVersion: String,
    OperatingSystem: String,
    IndexServerAddress: String,
    RegistryConfig: Internals.RegistryConfig,
    InitSha1: String,
    InitPath: String,
    NCPU: Int,
    MemTotal: Int,
    DockerRootDir: String,
    HttpProxy: String,
    HttpsProxy: String,
    NoProxy: String,
    Name: String,
    labels: Seq[String],
    ExperimentalBuild: Boolean
  )

  case class ContainerInspect
  (
    Id: String,
    Created: String,
    Path: String,
    Args: Seq[String],
    State: Internals.State,
    Image: String,
    NetworkSettings: Internals.NetworkSettings,
    ResolvConfPath: String,
    HostnamePath: String,
    HostsPath: String,
    LogPath: String,
    Name: String,
    RestartCount: Int,
    Driver: String,
    ExecDriver: String,
    MountLabel: String,
    ProcessLabel: String,
    Volumes: Map[String, String],
    VolumesRW: Map[String, Boolean],
    AppArmorProfile: String,
    ExecIDs: Option[Seq[String]],
    HostConfig: Internals.HostConfig,
    Config: Internals.Config
  )

  case class ContainerTop
  (
    Titles: Seq[String],
    Processes: Seq[Seq[String]]
  )

  case class Stat
  (
    read: String,
    network_stat: Internals.NetworkStat,
    precpu_stats: Internals.CpuStat,
    cpu_stats: Internals.CpuStat,
    memory_stats: Internals.MemoryStats,
    blkio_stats: Internals.BlkioStats
  )

  // List Responses
  case class Images(list: Seq[Internals.Image])
  case class ImageHistory(list: Seq[Internals.History])
  case class Containers(list: Seq[Internals.Container])
  case class ContainerChanges(list: Seq[Internals.ContainerChange])

  sealed trait Error
  // Errors
  case class UnexpectedError(msg: String) extends Error
  case class NotFound(msg: String) extends Error
  case class BadParameter(msg: String) extends Error
  case class ServerError(msg: String) extends Error

  // Failures
  sealed trait Failure extends Error { val cause: Throwable }
  case class ConnectionFailed(cause: Throwable) extends Failure

  object Internals {
    case class RegistryConfig
    (
      InsecureRegistryCIDRs: Seq[String],
      IndexConfigs: Map[String, IndexConfig]
    )
    case class IndexConfig
    (
      Name: String,
      Mirrors: Option[Seq[String]],
      Secure: Boolean,
      Official: Boolean
    )

    case class Image
    (
      Id: String,
      ParentId: String,
      RepoTags: Seq[String],
      RepoDigests: Option[Seq[String]],
      Created: BigInt,
      Size: Int,
      VirtualSize: Int,
      Labels: Option[Map[String, String]]
    )

    case class Container
    (
      Id: String,
      Names: Seq[String],
      Image: String,
      Command: String,
      Created: BigInt,
      Ports: Seq[ContainerPort],
      SizeRootFs: Option[BigInt],
      Labels: Option[Map[String, String]],
      Status: String
    )

    case class ContainerChange
    (
      Path: String,
      Kind: Int
    )

    case class ContainerPort
    (
      IP: String,
      PrivatePort: Int,
      PublicPort: Int,
      Type: String
    )

    case class State
    (
      Running: Boolean,
      Paused: Boolean,
      Restarting: Boolean,
      OOMKilled: Boolean,
      Dead: Boolean,
      Pid: Int,
      ExitCode: Int,
      Error: String,
      StartedAt: String,
      FinishedAt: String
    )

    case class NetworkSettings
    (
      Bridge: String,
      EndpointID: String,
      Gateway: String,
      GlobalIPv6Address: String,
      GlobalIPv6PrefixLen: Int,
      HairpinMode: Boolean,
      IPAddress: String,
      IPPrefixLen: Int,
      IPv6Gateway: String,
      LinkLocalIPv6Address: String,
      LinkLocalIPv6PrefixLen: Int,
      MacAddress: String,
      NetworkID: String,
      PortMapping: String, // DEBUG!!! Placeholder
      Ports: Map[String, Seq[NetworkSettingsPort]],
      SandboxKey: String,
      SecondaryIPAddresses: Option[String],
      SecondaryIPv6Addresses: Option[String]
    )

    case class HostConfig
    (
      Binds: Seq[String],
      ContainerIDFile: String,
      LxcConf: Seq[String],
      Memory: Int,
      MemorySwap: Int,
      CpuShares: Int,
      CpuPeriod: Int,
      CpusetCpus: String,
      CpusetMems: String,
      CpuQuota: Int,
      BlkioWeight: Int,
      OomKillDisable: Boolean,
      Privileged: Boolean,
      PortBindings: Map[String, Seq[NetworkSettingsPort]],
      Links: String, // DEBUG!!! Placeholder
      PublishAllPorts: Boolean,
      Dns: Option[Seq[String]], // DEBUG!!! Placeholder
      DnsSearch: Option[Seq[String]], // DEBUG!!! Placeholder
      ExtraHosts: Option[Seq[String]], // DEBUG!!! Placeholder
      VolumesFrom: Option[Seq[String]], // DEBUG!!! Placeholder
      Devices: Option[Seq[Device]],
      NetworkMode: String,
      IpcMode: String,
      PidMode: String,
      UTSMode: String,
      CapAdd: Option[Seq[String]],
      CapDrop: Option[Seq[String]],
      RestartPolicy: RestartPolicy,
      SecurityOpt: Option[Seq[String]], // DEBUG!!! Placeholder
      ReadonlyRootfs: Boolean,
      Ulimits: Option[Seq[ULimit]],
      LogConfig: LogConfig
    )

    case class NetworkSettingsPort
    (
      HostIp: String,
      HostPort: String
    )

    case class RestartPolicy
    (
      Name: String,
      MaximumRetryCount: Int
    )

    case class LogConfig
    (
      Type: String,
      Config: Map[String, String]
    )

    case class Config
    (
      Hostname: String,
      Domainname: String,
      User: String,
      AttachStdin: Boolean,
      AttachStdout: Boolean,
      AttachStderr: Boolean,
      PortSpecs: String, // DEBUG!!! Placeholder
      ExposedPorts: Map[String, Any], // DEBUG!!! Placeholder
      Tty: Boolean,
      OpenStdin: Boolean,
      StdinOnce: Boolean,
      Env: Seq[String],
      Cmd: Seq[String],
      Image: String,
      Volumes: String, // DEBUG!!! Placeholder
      VolumeDriver: String,
      WorkingDir: String,
      Entrypoint: String,
      NetworkDisabled: Boolean,
      MacAddress: String,
      OnBuild: Option[String], // DEBUG!!! Placeholder
      Labels: Option[Map[String, String]] // DEBUG!!! Placeholder
    )

    case class Device
    (
      PathOnHost: String,
      PathInContainer: String,
      CgroupPermissions: String
    )

    case class ULimit
    (
      Name: String,
      Soft: String,
      Hard: String
    )

    case class NetworkStat
    (
      rx_bytes: Int,
      rx_packets: Int,
      rx_errors: Int,
      rx_dropped: Int,
      tx_bytes: Int,
      tx_packets: Int,
      tx_errors: Int,
      tx_dropped: Int
    )

    case class CpuStat
    (
      cpu_usage: CpuUsage,
      system_cpu_usage: BigInt,
      throttling_data: ThrottlingData
    )

    case class CpuUsage
    (
      total_usage: BigInt,
      percpu_usage: Seq[BigInt],
      usage_in_kernelmode: BigInt,
      usage_in_usermode: BigInt
    )

    case class ThrottlingData
    (
      periods: Int,
      throttled_periods: Int,
      throttled_time: Int
    )

    case class MemoryStats
    (
      usage: BigInt,
      max_usage: BigInt,
      stats: DetailedMemoryStats,
      failcnt: Int,
      limit: BigInt
    )

    case class DetailedMemoryStats
    (
      active_anon: Int,
      active_file: Int,
      cache: Int,
      hierarchical_memory_limit: BigInt,
      hierarchical_memsw_limit: BigInt,
      inactive_anon: Int,
      inactive_file: Int,
      mapped_file: Int,
      pgfault: Int,
      pgmajfault: Int,
      pgpgin: Int,
      pgpgout: Int,
      rss: Int,
      rss_huge: Int,
      swap: Int,
      total_active_anon: Int,
      total_active_file: Int,
      total_cache: Int,
      total_inactive_anon: Int,
      total_inactive_file: Int,
      total_mapped_file: Int,
      total_pgfault: Int,
      total_pgmajfault: Int,
      total_pgpgin: Int,
      total_pgpgout: Int,
      total_rss: Int,
      total_rss_huge: Int,
      total_swap: Int,
      total_unevictable: Int,
      total_writeback: Int,
      unevictable: Int,
      writeback: Int
    )

    case class BlkioStats
    (
      io_service_bytes_recursive: Option[Seq[Int]],
      io_serviced_recursive: Option[Seq[Int]],
      io_queue_recursive: Option[Seq[Int]],
      io_service_time_recursive: Option[Seq[Int]],
      io_wait_time_recursive: Option[Seq[Int]],
      io_merged_recursive: Option[Seq[Int]],
      io_time_recursive: Option[Seq[Int]],
      sectors_recursive: Option[Seq[Int]]
    )

    case class History
    (
      Id: String,
      Created: BigInt,
      CreatedBy: String,
      Tags: Option[Seq[String]],
      Size: BigInt,
      Comment: String
    )

  }

  object Implicits {
    implicit val formats = Serialization.formats(NoTypeHints)
  }
}
