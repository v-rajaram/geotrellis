/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.s3.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import java.net.URI

import com.typesafe.config.ConfigFactory

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class S3COGLayerReader(
  val attributeStore: AttributeStore,
  val bucket: String,
  val prefix: String,
  val getS3Client: () => S3Client = () => S3Client.DEFAULT,
  val defaultThreads: Int = ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.read")
)(@transient implicit val sc: SparkContext) extends FilteringCOGLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions: Int = sc.defaultParallelism

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, getS3Client())

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], numPartitions: Int, filterIndexOnly: Boolean) = {
    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      (index: BigInt) => s"$bucket/${makePath(prefix, Index.encode(index, maxWidth))}.${Extension}"

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      numPartitions   = numPartitions,
      filterIndexOnly = filterIndexOnly,
      getKeyPath      = getKeyPath,
      pathExists      = { s3PathExists(_, getS3Client()) },
      fullPath        = { path => new URI(s"s3://$path") },
      defaultThreads  = defaultThreads
    )
  }

}